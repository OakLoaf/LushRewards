package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.HashMultimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

public class DailyRewardsGui extends AbstractGui {
    private static final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");
    private final DailyRewardsModule dailyRewardsModule;
    private final GuiFormat.GuiTemplate guiTemplate;

    public DailyRewardsGui(DailyRewardsModule dailyRewardsModule, Player player) {
        super(dailyRewardsModule.getGuiFormat().getTemplate().getRowCount() * 9, ChatColorHandler.translateAlternateColorCodes(dailyRewardsModule.getGuiFormat().getTitle()), player);
        this.guiTemplate = dailyRewardsModule.getGuiFormat().getTemplate();
        this.dailyRewardsModule = dailyRewardsModule;
    }

    @Override
    public void recalculateContents() {
        inventory.clear();
        clearButtons();

        // Gets RewardUser
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);

        // Checks if the streak mode config option is enabled
        if (ActivityRewarder.getConfigManager().isStreakModeEnabled()) {
            // Resets RewardUser to Day 1 if a day has been missed
            if (rewardUser.getLastDate().isBefore(LocalDate.now().minusDays(1))) {
                rewardUser.resetDays();
            }
        }

        // Checks if the reward has been collected today
        boolean collectedToday = rewardUser.hasCollectedToday();
        // The current day number being shown to the user
        int currDayNum = collectedToday ? rewardUser.getDayNum() - 1 : rewardUser.getDayNum();
        // The day number that the user is technically on
        int actualDayNum = rewardUser.getActualDayNum();

        // First reward day shown
        AtomicInteger dayIndex = new AtomicInteger();
        SimpleDate dateIndex;
        switch (dailyRewardsModule.getScrollType()) {
            case GRID -> {
                int rewardDisplaySize = guiTemplate.countChar('R');

                int endDay = rewardDisplaySize;
                while (currDayNum > endDay) {
                    endDay += rewardDisplaySize;
                }

                dayIndex.set(endDay - (rewardDisplaySize - 1));

                int diff = dayIndex.get() - currDayNum;
                dateIndex = SimpleDate.now();
                dateIndex.addDays(diff);
            }
            case MONTH -> {
                SimpleDate today = SimpleDate.now();
                dateIndex = new SimpleDate(1, today.getMonth(), today.getYear());
                dayIndex.set(currDayNum - (today.getDay() - 1));
            }
            default -> {
                dayIndex.set(currDayNum);
                dateIndex = SimpleDate.now();
            }
        }

        HashMultimap<Character, Integer> slotMap = HashMultimap.create();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            char character = guiTemplate.getCharAt(slot);
            if (character == 'N') character = 'U';

            slotMap.put(character, slot);
        }

        for (Character character : slotMap.keySet()) {
            switch (character) {
                case 'R' -> slotMap.get(character).forEach(slot -> {
                    // Get the day's reward for the current slot
                    DailyRewardCollection reward = dailyRewardsModule.getRewardDay(dateIndex, dayIndex.get()).getHighestPriorityRewardCollection();
                    // TODO: Store list of collected days in RewardUser to check for collected vs missed days

                    String itemTemplate = (dayIndex.get() == currDayNum && collectedToday) ? "collected-reward" : "default-reward";

                    SimpleItemStack displayItem = SimpleItemStack.overwrite(reward.getDisplayItem(), ActivityRewarder.getConfigManager().getCategoryTemplate(reward.getCategory()));
                    displayItem = SimpleItemStack.overwrite(displayItem, ActivityRewarder.getConfigManager().getItemTemplate(itemTemplate));

                    if (displayItem.getDisplayName() != null) {
                        displayItem.setDisplayName(displayItem.getDisplayName()
                                .replaceAll("%day%", String.valueOf(dayIndex.get()))
                                .replaceAll("%month_day%", String.valueOf(dateIndex.getDay())));
                    }
                    displayItem.parseColors(player);

                    ItemStack itemStack;
                    try {
                        itemStack = displayItem.getItemStack(player);
                    } catch (IllegalArgumentException e) {
                        ActivityRewarder.getInstance().getLogger().severe("Failed to display item-template '" + itemTemplate + "' as it does not specify a valid material");
                        itemStack = new ItemStack(Material.STONE);
                    }

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta != null) {
                        // Changes item data based on if the reward has been collected or not
                        if (dayIndex.get() == currDayNum && collectedToday) {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex.get() + rewardUser.getDayNumOffset()) + "|unavailable"));
                        } else {
                            addButton(slot, (event) -> {
                                // Gets clicked item and checks if it exists
                                ItemStack currItem = event.getCurrentItem();
                                if (currItem == null) {
                                    return;
                                }

                                removeButton(slot);

                                ItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), ActivityRewarder.getConfigManager().getItemTemplate("collected-reward")).getItemStack(player);
                                inventory.setItem(slot, collectedItem);

                                Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

                                Debugger.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), Debugger.DebugMode.DAILY);
                                RewardDay rewardDay = RewardDay.from(dailyRewardsModule.getStreakRewards(dayIndex.get()));
                                DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();
                                if (ActivityRewarder.getConfigManager().shouldStackRewards()) {
                                    rewardDay.giveAllRewards(player);
                                } else {
                                    priorityReward.giveAll(player);
                                }

                                Debugger.sendDebugMessage("Successfully gave rewards to " + player.getName(), Debugger.DebugMode.DAILY);
                                ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("daily-reward-given"));

                                Debugger.sendDebugMessage("Attempting to send playtime rewards to " + player.getName(), Debugger.DebugMode.PLAYTIME);

                                if (ActivityRewarder.getModule("playtime-global-goals") instanceof PlaytimeGlobalGoalsModule globalGoalsModule && globalGoalsModule.shouldReceiveWithDailyRewards()) {
                                    RewardCollection hourlyRewards = globalGoalsModule.getRewardCollection(rewardUser.getPlayHours());
                                    if (hourlyRewards != null && !hourlyRewards.isEmpty()) {
                                        Debugger.sendDebugMessage("Attempting to give rewards to player", Debugger.DebugMode.PLAYTIME);
                                        hourlyRewards.giveAll(player);
                                        Debugger.sendDebugMessage("Successfully gave player rewards", Debugger.DebugMode.PLAYTIME);
                                    }
                                }

                                player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
                                rewardUser.incrementDayNum();
                                rewardUser.setLastDate(LocalDate.now().toString());
                            });
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex.get() + rewardUser.getDayNumOffset()) + "|collectable"));
                        }
                    }

                    // Sets the size of the stack to the same amount as the current date
                    if (dailyRewardsModule.showDateAsAmount()) {
                        itemStack.setAmount(dateIndex.getDay());
                    }

                    inventory.setItem(slot, itemStack);

                    dayIndex.getAndIncrement();
                    dateIndex.addDays(1);
                });
                case 'U', 'N' -> {
                    String upcomingCategory = ActivityRewarder.getConfigManager().getUpcomingCategory();
                    int upcomingRewardDay = dailyRewardsModule.findNextRewardFromCategory(dayIndex.get(), upcomingCategory);
                    SimpleDate upcomingRewardDate = rewardUser.getDateOnDayNum(upcomingRewardDay);

                    // Adds the upcoming reward to the GUI if it exists
                    if (upcomingRewardDay != -1) {
                        SimpleItemStack categoryItem = ActivityRewarder.getConfigManager().getCategoryTemplate(upcomingCategory);

                        // Get the day's reward for the current slot
                        DailyRewardCollection upcomingReward = dailyRewardsModule.getRewardDay(upcomingRewardDate, upcomingRewardDay).getHighestPriorityRewardCollection();
                        SimpleItemStack simpleItemStack = SimpleItemStack.overwrite(categoryItem, ActivityRewarder.getConfigManager().getItemTemplate("upcoming-reward"));
                        simpleItemStack = SimpleItemStack.overwrite(simpleItemStack, upcomingReward.getDisplayItem());

                        if (simpleItemStack.getDisplayName() != null) {
                            simpleItemStack.setDisplayName(ChatColorHandler.translateAlternateColorCodes(simpleItemStack
                                            .getDisplayName()
                                            .replaceAll("%day%", String.valueOf(upcomingRewardDay - rewardUser.getDayNumOffset())),
                                    player));
                        }
                        simpleItemStack.setLore(ChatColorHandler.translateAlternateColorCodes(simpleItemStack.getLore(), player));

                        ItemStack itemStack = simpleItemStack.getItemStack(player);
                        ItemMeta itemMeta = itemStack.getItemMeta();
                        if (itemMeta != null) {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, ((upcomingRewardDay - rewardUser.getDayNumOffset()) + "|" + actualDayNum + "|unavailable"));
                            itemStack.setItemMeta(itemMeta);
                        }

                        slotMap.get(character).forEach(slot -> inventory.setItem(slot, itemStack));
                    }
                }
                default -> slotMap.get(character).forEach(slot -> {
                    SimpleItemStack simpleItemStack = ActivityRewarder.getConfigManager().getItemTemplate(String.valueOf(character));

                    if (!simpleItemStack.hasType()) {
                        simpleItemStack.setType(Material.STONE);
                        ActivityRewarder.getInstance().getLogger().severe("Failed to display custom item-template '" + character + "' as it does not specify a valid material");
                    }
                    simpleItemStack.parseColors(player);

                    inventory.setItem(slot, simpleItemStack.getItemStack(player));
                });
            }
        }
    }

    public enum ScrollType {
        DAY,
        MONTH,
        GRID
    }
}