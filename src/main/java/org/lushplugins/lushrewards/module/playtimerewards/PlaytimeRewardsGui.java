package org.lushplugins.lushrewards.module.playtimerewards;

import com.google.common.collect.TreeMultimap;
import org.bukkit.inventory.Inventory;
import org.lushplugins.lushlib.utils.DisplayItemStack;
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
    public void refresh() {
        Inventory inventory = this.getInventory();
        Player player = this.getPlayer();

        inventory.clear();
        clearButtons();

        LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(playtimeTrackerModule -> {
            PlaytimeTracker playtimeTracker = ((PlaytimeTrackerModule) playtimeTrackerModule).getPlaytimeTracker(player.getUniqueId());

            module.getOrLoadUserData(player.getUniqueId(), true)
                .completeOnTimeout(null, 15, TimeUnit.SECONDS)
                .thenAccept(userData -> LushRewards.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                    if (userData == null) {
                        DisplayItemStack errorItem = DisplayItemStack.builder(Material.BARRIER)
                            .setDisplayName("&#ff6969Failed to load rewards user data try relogging")
                            .setLore(List.of("&7&oIf this continues please", "&7&oreport to your server administrator"))
                            .build();

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
                                DisplayItemStack displayItem = LushRewards.getInstance().getConfigManager().getItemTemplate("claim-all", module);
                                if (module.hasClaimableRewards(player)) {
                                    displayItem = DisplayItemStack.builder(displayItem)
                                        .setEnchantGlow(true)
                                        .build();
                                }

                                inventory.setItem(slot, displayItem.asItemStack(player, true));

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

                                DisplayItemStack.Builder displayItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getCategoryTemplate(reward.getCategory()))
                                    .overwrite(DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate(itemTemplate, module)))
                                    .overwrite(DisplayItemStack.builder(reward.getDisplayItem()));

                                if (displayItemBuilder.getDisplayName() != null) {
                                    displayItemBuilder.setDisplayName(displayItemBuilder.getDisplayName()
                                        .replace("%minutes%", String.valueOf(minutes)));
                                }

                                if (displayItemBuilder.hasLore()) {
                                    displayItemBuilder.setLore(displayItemBuilder.getLore().stream().map(line ->
                                        line.replace("%minutes%", String.valueOf(minutes))
                                    ).toList());
                                }

                                displayItemBuilder.parseColors(player);

                                if (module.hasClaimableRewards(player, playtimeTracker.getGlobalPlaytime())) {
                                    addButton(slot, (event) -> {
                                        // Gets clicked item and checks if it exists
                                        ItemStack currItem = event.getCurrentItem();
                                        if (currItem == null) {
                                            return;
                                        }

                                        removeButton(slot);

                                        DisplayItemStack.Builder collectedItemBuilder = DisplayItemStack.builder(currItem)
                                            .overwrite(DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward", module)));

                                        if (collectedItemBuilder.getDisplayName() != null) {
                                            collectedItemBuilder.setDisplayName(collectedItemBuilder.getDisplayName()
                                                .replace("%minutes%", String.valueOf(minutes)));
                                        }

                                        inventory.setItem(slot, collectedItemBuilder.build().asItemStack(player, true));

                                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);
                                        if (module.hasClaimableRewards(player, playtimeTracker.getGlobalPlaytime())) {
                                            module.claimRewards(player, playtimeTracker.getGlobalPlaytime());
                                        }

                                        refresh();
                                    });
                                }

                                inventory.setItem(slot, displayItemBuilder.build().asItemStack(player, true));
                            });
                            case ' ' ->
                                slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
                            default -> slotMap.get(character).forEach(slot -> {
                                DisplayItemStack displayItem = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf(character), module);

                                if (!displayItem.hasType()) {
                                    displayItem = DisplayItemStack.builder(displayItem)
                                        .setType(Material.RED_STAINED_GLASS_PANE)
                                        .build();

                                    LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '" + character + "' as it does not specify a valid material");
                                }

                                inventory.setItem(slot, displayItem.asItemStack(player, true));
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
