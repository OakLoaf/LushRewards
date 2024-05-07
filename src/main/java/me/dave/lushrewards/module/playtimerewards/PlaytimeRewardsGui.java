package me.dave.lushrewards.module.playtimerewards;

import com.google.common.collect.TreeMultimap;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTracker;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.lushrewards.rewards.collections.PlaytimeRewardCollection;
import me.dave.lushrewards.utils.Debugger;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.gui.inventory.Gui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.Pair;
import org.lushplugins.lushlib.utils.SimpleItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public class PlaytimeRewardsGui extends Gui {
    private final PlaytimeRewardsModule module;
    private final GuiFormat.GuiTemplate guiTemplate;
    private final PriorityQueue<Pair<PlaytimeRewardCollection, Integer>> rewardQueue = new PriorityQueue<>(Comparator.comparingInt(Pair::second));

    public PlaytimeRewardsGui(PlaytimeRewardsModule module, Player player) {
        super(module.getGuiFormat().getTemplate().getRowCount() * 9, ChatColorHandler.translate(module.getGuiFormat().getTitle()), player);
        this.module = module;
        this.guiTemplate = module.getGuiFormat().getTemplate();
    }

    @Override
    public void recalculateContents() {
        inventory.clear();
        clearButtons();

        LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(playtimeTrackerModule -> {
            PlaytimeTracker playtimeTracker = ((PlaytimeTrackerModule) playtimeTrackerModule).getPlaytimeTracker(player.getUniqueId());

            module.getOrLoadUserData(player.getUniqueId(), true)
                .completeOnTimeout(null, 15, TimeUnit.SECONDS)
                .thenAccept(userData -> Bukkit.getScheduler().runTask(LushRewards.getInstance(), () -> {
                    if (userData == null) {
                        SimpleItemStack errorItem = new SimpleItemStack(Material.BARRIER);
                        errorItem.setDisplayName("&#ff6969Failed to load rewards user data try relogging");
                        errorItem.setLore(List.of("&7&oIf this continues please", "&7&oreport to your server administrator"));
                        errorItem.parseColors(player);

                        inventory.setItem(4, errorItem.asItemStack());

                        return;
                    }

                    int playtime = playtimeTracker.getGlobalPlaytime();
                    int startPlaytime = (int) ((playtime - module.getShortestRepeatFrequency(playtime)) * Math.floor(guiTemplate.countChar('R') / 2D));
                    module.getRewards().forEach(reward -> {
                        if (!reward.shouldHideFromGui()) {
                            Integer minutes = findFirstNumInSequence(reward.getStartMinute(), reward.getRepeatFrequency(), startPlaytime);
                            if (minutes != null) {
                                rewardQueue.add(new Pair<>(reward, minutes));
                            }
                        }
                    });

                    TreeMultimap<Character, Integer> slotMap = guiTemplate.getSlotMap();
                    for (Character character : slotMap.keySet()) {
                        switch (character) {
                            case 'A' -> slotMap.get(character).forEach(slot -> {
                                SimpleItemStack simpleItemStack = LushRewards.getInstance().getConfigManager().getItemTemplate("claim-all", module);
                                if (module.hasClaimableRewards(player)) {
                                    simpleItemStack.setEnchantGlow(true);
                                }

                                inventory.setItem(slot, simpleItemStack.asItemStack(player, true));

                                addButton(slot, event -> {
                                    removeButton(slot);

                                    if (module.hasClaimableRewards(player)) {
                                        module.claimRewards(player);
                                    }
                                });
                            });
                            case 'R' -> slotMap.get(character).forEach(slot -> {
                                if (rewardQueue.isEmpty()) {
                                    return;
                                }

                                Pair<PlaytimeRewardCollection, Integer> rewardPair = rewardQueue.poll();
                                PlaytimeRewardCollection reward = rewardPair.first();
                                int minutes = rewardPair.second();
                                int nextMinute = minutes + reward.getRepeatFrequency();

                                if (reward.getRepeatFrequency() > 0 && nextMinute <= reward.getRepeatsUntil()) {
                                    rewardQueue.add(new Pair<>(reward, nextMinute));
                                }

                                String itemTemplate;
                                if (minutes < userData.getLastCollectedPlaytime()) {
                                    itemTemplate = "collected-reward";
                                } else if (playtimeTracker.getGlobalPlaytime() > minutes) {
                                    itemTemplate = "redeemable-reward";
                                } else {
                                    itemTemplate = "default-reward";
                                }

                                SimpleItemStack displayItem = SimpleItemStack.overwrite(LushRewards.getInstance().getConfigManager().getCategoryTemplate(reward.getCategory()), LushRewards.getInstance().getConfigManager().getItemTemplate(itemTemplate, module), reward.getDisplayItem());
                                if (displayItem.getDisplayName() != null) {
                                    displayItem.setDisplayName(displayItem.getDisplayName()
                                        .replace("%minutes%", String.valueOf(minutes)));
                                }

                                if (displayItem.getLore() != null) {
                                    displayItem.setLore(displayItem.getLore().stream().map(line ->
                                        line.replace("%minutes%", String.valueOf(minutes))
                                    ).toList());
                                }

                                displayItem.parseColors(player);

                                if (module.hasClaimableRewards(player)) {
                                    addButton(slot, (event) -> {
                                        // Gets clicked item and checks if it exists
                                        ItemStack currItem = event.getCurrentItem();
                                        if (currItem == null) {
                                            return;
                                        }

                                        removeButton(slot);

                                        SimpleItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward", module));
                                        if (collectedItem.getDisplayName() != null) {
                                            collectedItem.setDisplayName(collectedItem.getDisplayName()
                                                .replace("%minutes%", String.valueOf(minutes)));
                                        }
                                        collectedItem.parseColors(player);

                                        inventory.setItem(slot, collectedItem.asItemStack());

                                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

                                        // TODO: Make give only clicked (and previous) rewards
                                        if (module.hasClaimableRewards(player)) {
                                            module.claimRewards(player);
                                        }
                                    });
                                }

                                inventory.setItem(slot, displayItem.asItemStack());
                            });
                            case ' ' ->
                                slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
                            default -> slotMap.get(character).forEach(slot -> {
                                SimpleItemStack simpleItemStack = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf(character), module);

                                if (!simpleItemStack.hasType()) {
                                    simpleItemStack.setType(Material.RED_STAINED_GLASS_PANE);
                                    LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '" + character + "' as it does not specify a valid material");
                                }
                                simpleItemStack.parseColors(player);

                                inventory.setItem(slot, simpleItemStack.asItemStack(player));
                            });
                        }
                    }
                }));
        });
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        super.onClick(event, true);
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        super.onClose(event);
        rewardQueue.clear();
    }

    @Nullable
    private Integer findFirstNumInSequence(int start, int increment, int lowerBound) {
        if (increment <= 0) {
            return start > lowerBound ? start : null;
        }

        return (int) (start + increment * Math.ceil((lowerBound - start) / increment));
    }
}
