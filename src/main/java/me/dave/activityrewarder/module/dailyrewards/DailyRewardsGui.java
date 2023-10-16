package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.TreeMultimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
        // Gets ModuleUserData
        if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModuleUserData moduleUserData)) {
            SimpleItemStack errorItem = new SimpleItemStack(Material.BARRIER);
            errorItem.setDisplayName("&#ff6969Failed to load rewards user data try relogging");
            errorItem.setLore(List.of("&7&oIf this continues please", "report to your server administrator"));
            errorItem.parseColors(player);

            inventory.setItem(4, errorItem.getItemStack());

            return;
        }

        // Checks if the streak mode config option is enabled
        if (dailyRewardsModule.isStreakModeEnabled()) {
            // Resets RewardUser to Day 1 if a day has been missed
            LocalDate lastCollectedDate = moduleUserData.getLastCollectedDate();
            if (lastCollectedDate == null || (lastCollectedDate.isBefore(LocalDate.now().minusDays(1)) && !lastCollectedDate.isEqual(LocalDate.of(1971, 10, 1)))) {
                moduleUserData.restartStreak();
                rewardUser.save();
            }
        }

        // Checks if the reward has been collected today
        boolean collectedToday = moduleUserData.hasCollectedToday();
        // The current day number being shown to the user
        int currDayNum = moduleUserData.getDayNum();

        // First reward day shown
        AtomicInteger dayIndex = new AtomicInteger();
        final LocalDate[] dateIndex = new LocalDate[1];
        switch (dailyRewardsModule.getScrollType()) {
            case GRID -> {
                int rewardDisplaySize = guiTemplate.countChar('R');

                int endDay = rewardDisplaySize;
                while (currDayNum > endDay) {
                    endDay += rewardDisplaySize;
                }

                dayIndex.set(endDay - (rewardDisplaySize - 1));

                int diff = dayIndex.get() - currDayNum;
                dateIndex[0] = LocalDate.now().plusDays(diff);
            }
            case MONTH -> {
                LocalDate today = LocalDate.now();
                dateIndex[0] = LocalDate.of(today.getYear(), today.getMonth(), 1);
                dayIndex.set(currDayNum - (today.getDayOfMonth() - 1));
            }
            default -> {
                dayIndex.set(currDayNum);
                dateIndex[0] = LocalDate.now();
            }
        }

        TreeMultimap<Character, Integer> slotMap = TreeMultimap.create();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            char character = guiTemplate.getCharAt(slot);
            if (character == 'N') character = 'U';

            slotMap.put(character, slot);
        }

        HashSet<String> collectedDates = moduleUserData.getCollectedDates();
        for (Character character : slotMap.keySet()) {
            switch (character) {
                case 'R' -> slotMap.get(character).forEach(slot -> {
                    ItemStack itemStack;
                    if (dailyRewardsModule.getScrollType().equals(ScrollType.MONTH) && dateIndex[0].getMonthValue() != LocalDate.now().getMonthValue()) {
                        SimpleItemStack simpleItemStack = ActivityRewarder.getConfigManager().getItemTemplate(String.valueOf('#'));

                        if (!simpleItemStack.hasType()) {
                            simpleItemStack.setType(Material.STONE);
                            ActivityRewarder.getInstance().getLogger().severe("Failed to display custom item-template '#' as it does not specify a valid material");
                        }
                        simpleItemStack.parseColors(player);

                        itemStack = simpleItemStack.getItemStack(player);
                    } else {
                        // Get the day's reward for the current slot
                        RewardDay rewardDay = dailyRewardsModule.getRewardDay(dateIndex[0], dayIndex.get());
                        DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

                        String itemTemplate;
                        if (dayIndex.get() < currDayNum) {
                            itemTemplate = (collectedDates.contains(dateIndex[0].format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))) ? "collected-reward" : "missed-reward";
                        } else if (dayIndex.get() == currDayNum) {
                            itemTemplate = collectedToday ? "collected-reward" : "redeemable-reward";
                        } else {
                            itemTemplate = "default-reward";
                        }

                        SimpleItemStack displayItem;
                        if (!rewardDay.isEmpty()) {
                            displayItem = SimpleItemStack.overwrite(ActivityRewarder.getConfigManager().getCategoryTemplate(priorityReward.getCategory()), ActivityRewarder.getConfigManager().getItemTemplate(itemTemplate), priorityReward.getDisplayItem());

                            if (displayItem.getDisplayName() != null) {
                                displayItem.setDisplayName(displayItem.getDisplayName()
                                    .replaceAll("%day%", String.valueOf(dayIndex.get()))
                                    .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                    .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                    .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                    .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                    .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                    .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                            }

                            if (displayItem.getLore() != null) {
                                displayItem.setLore(displayItem.getLore().stream().map(line ->
                                    line.replaceAll("%day%", String.valueOf(dayIndex.get()))
                                        .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                        .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                        .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                        .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                        .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                        .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                ).toList());
                            }

                            displayItem.parseColors(player);
                        } else {
                            displayItem = new SimpleItemStack(Material.AIR);
                        }

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
                                        .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                        .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                        .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                        .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                        .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                        .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                                }
                                collectedItem.parseColors(player);

                                inventory.setItem(slot, collectedItem.getItemStack());

                                Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

                               dailyRewardsModule.claimRewards(player);

                                if (ActivityRewarder.getModule(PlaytimeDailyGoalsModule.ID) instanceof PlaytimeDailyGoalsModule dailyGoalsModule && dailyGoalsModule.shouldReceiveWithDailyRewards()) {
                                    dailyGoalsModule.claimRewards(player);
                                }

                                if (ActivityRewarder.getModule(PlaytimeGlobalGoalsModule.ID) instanceof PlaytimeGlobalGoalsModule globalGoalsModule && globalGoalsModule.shouldReceiveWithDailyRewards()) {
                                    globalGoalsModule.claimRewards(player);
                                }

                                player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
                                moduleUserData.incrementStreakLength();
                                moduleUserData.setLastCollectedDate(LocalDate.now());
                                moduleUserData.addCollectedDate(LocalDate.now());
                                rewardUser.save();
                            });
                        }

                        // Sets the size of the stack to the same amount as the current date
                        if (dailyRewardsModule.showDateAsAmount()) {
                            itemStack.setAmount(dateIndex[0].getDayOfMonth());
                        }
                    }

                    inventory.setItem(slot, itemStack);

                    dayIndex.getAndIncrement();
                    dateIndex[0] = dateIndex[0].plusDays(1);
                });
                case 'U', 'N' -> {
                    String upcomingCategory = dailyRewardsModule.getUpcomingCategory();
                    Optional<DailyRewardCollection> upcomingReward = dailyRewardsModule.findNextRewardFromCategory(dayIndex.get(), dateIndex[0], upcomingCategory);

                    // Adds the upcoming reward to the GUI if it exists
                    if (upcomingReward.isPresent()) {
                        DailyRewardCollection upcomingRewardCollection = upcomingReward.get();
                        SimpleItemStack categoryItem = ActivityRewarder.getConfigManager().getCategoryTemplate(upcomingCategory);

                        // Get the day's reward for the current slot
                        SimpleItemStack simpleItemStack = SimpleItemStack.overwrite(categoryItem, ActivityRewarder.getConfigManager().getItemTemplate("upcoming-reward"), upcomingRewardCollection.getDisplayItem());

                        if (simpleItemStack.getDisplayName() != null) {
                            simpleItemStack.setDisplayName(ChatColorHandler.translateAlternateColorCodes(simpleItemStack
                                    .getDisplayName()
                                    .replaceAll("%day%", String.valueOf(upcomingRewardCollection.getRewardDayNum()))
                                    .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                    .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                    .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                    .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                    .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                    .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))),
                                player));
                        }
                        simpleItemStack.setLore(ChatColorHandler.translateAlternateColorCodes(simpleItemStack.getLore(), player));

                        ItemStack itemStack = simpleItemStack.getItemStack(player);
                        slotMap.get(character).forEach(slot -> inventory.setItem(slot, itemStack));
                    }
                }
                case ' ' -> slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
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