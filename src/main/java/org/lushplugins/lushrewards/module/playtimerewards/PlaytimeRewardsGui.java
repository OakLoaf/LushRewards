package org.lushplugins.lushrewards.module.playtimerewards;

import com.google.common.collect.TreeMultimap;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.gui.GuiFormat;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTracker;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushrewards.utils.Debugger;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.lushplugins.lushlib.gui.inventory.Gui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.Pair;
import org.lushplugins.lushlib.utils.SimpleItemStack;
import org.lushplugins.lushrewards.utils.MathUtils;

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
                .thenAccept(userData -> LushRewards.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    if (userData == null) {
                        SimpleItemStack errorItem = new SimpleItemStack(Material.BARRIER);
                        errorItem.setDisplayName("&#ff6969Failed to load rewards user data try relogging");
                        errorItem.setLore(List.of("&7&oIf this continues please", "&7&oreport to your server administrator"));
                        errorItem.parseColors(player);

                        inventory.setItem(4, errorItem.asItemStack(player, true));

                        return;
                    }

                    int playtime = playtimeTracker.getGlobalPlaytime() - userData.getPreviousDayEndPlaytime();
                    int lastCollectedPlaytime = userData.getLastCollectedPlaytime() - userData.getPreviousDayEndPlaytime();
                    Integer shortestFrequency = module.getShortestRepeatFrequency(playtime);
                    int startPlaytime;
                    switch (module.getScrollType()) {
                        case SCROLL -> {
                            if (shortestFrequency != null) {
                                startPlaytime = Math.max((int) (playtime - (shortestFrequency * Math.floor(guiTemplate.countChar('R') / 2D))), lastCollectedPlaytime);
                            } else {
                                startPlaytime = lastCollectedPlaytime;
                            }
                        }
                        default -> startPlaytime = 0;
                    }

                    module.getRewards().forEach(reward -> {
                        if (!reward.shouldHideFromGui()) {
                            Integer minutes = MathUtils.findFirstNumInSequence(reward.getStartMinute(), reward.getRepeatFrequency(), startPlaytime);
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
                                if (minutes < lastCollectedPlaytime) {
                                    itemTemplate = "collected-reward";
                                } else if (playtime > minutes) {
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

                                if (module.hasClaimableRewards(player, playtimeTracker.getGlobalPlaytime())) {
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

                                        inventory.setItem(slot, collectedItem.asItemStack(player, true));

                                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);
                                        if (module.hasClaimableRewards(player, playtimeTracker.getGlobalPlaytime())) {
                                            module.claimRewards(player, playtimeTracker.getGlobalPlaytime());
                                        }

                                        recalculateContents();
                                    });
                                }

                                inventory.setItem(slot, displayItem.asItemStack(player, true));
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

    public enum ScrollType {
        FIXED,
        SCROLL
    }
}
