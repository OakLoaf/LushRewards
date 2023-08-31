package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.HashMultimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.module.playtimegoals.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DailyRewardsGui extends AbstractGui {
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
            if (rewardUser.getLastCollectedDate().isBefore(SimpleDate.now().minusDays(1))) {
                rewardUser.resetDays();
            }
        }

        // Checks if the reward has been collected today
        boolean collectedToday = rewardUser.hasCollectedToday();
        // The current day number being shown to the user
        int currDayNum = collectedToday ? rewardUser.getDayNum() - 1 : rewardUser.getDayNum();

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

        List<String> collectedDates = rewardUser.getCollectedDates();
        for (Character character : slotMap.keySet()) {
            switch (character) {
                case 'R' -> slotMap.get(character).forEach(slot -> {
                    // Get the day's reward for the current slot
                    DailyRewardCollection reward = dailyRewardsModule.getRewardDay(dateIndex, dayIndex.get()).getHighestPriorityRewardCollection();

                    String itemTemplate;
                    if (dayIndex.get() < currDayNum) {
                        itemTemplate = (collectedDates.contains(dateIndex.toString("dd-mm-yyyy"))) ? "collected-reward" : "missed-reward";
                    } else if (dayIndex.get() == currDayNum) {
                        itemTemplate = collectedToday ? "collected-reward" : "redeemable-reward";
                    } else {
                        itemTemplate = "default-reward";
                    }

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

                    // Changes item data based on if the reward has been collected or not
                    if (dayIndex.get() == currDayNum && !collectedToday) {
                        addButton(slot, (event) -> {
                            // Gets clicked item and checks if it exists
                            ItemStack currItem = event.getCurrentItem();
                            if (currItem == null) {
                                return;
                            }

                            removeButton(slot);

                            SimpleItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), ActivityRewarder.getConfigManager().getItemTemplate("collected-reward"));
                            if (collectedItem.getDisplayName() != null) {
                                collectedItem.setDisplayName(collectedItem.getDisplayName()
                                    .replaceAll("%day%", String.valueOf(currDayNum))
                                    .replaceAll("%month_day%", String.valueOf(SimpleDate.now())));
                            }
                            collectedItem.parseColors(player);

                            inventory.setItem(slot, collectedItem.getItemStack());

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
                                RewardCollection hourlyRewards = globalGoalsModule.getRewardCollection(rewardUser.getHoursPlayed());
                                if (hourlyRewards != null && !hourlyRewards.isEmpty()) {
                                    Debugger.sendDebugMessage("Attempting to give rewards to player", Debugger.DebugMode.PLAYTIME);
                                    hourlyRewards.giveAll(player);
                                    Debugger.sendDebugMessage("Successfully gave player rewards", Debugger.DebugMode.PLAYTIME);
                                }
                            }

                            player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
                            rewardUser.incrementDayNum();
                            rewardUser.setLastDate(SimpleDate.now());
                            rewardUser.addCollectedDate(SimpleDate.now());
                        });
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

    @Override
    public void onClick(InventoryClickEvent event) {
        super.onClick(event, true);
    }

    public enum ScrollType {
        DAY,
        MONTH,
        GRID
    }
}