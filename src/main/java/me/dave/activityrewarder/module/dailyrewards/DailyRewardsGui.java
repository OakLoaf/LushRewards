package me.dave.activityrewarder.module.dailyrewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.InventoryHandler;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class DailyRewardsGui extends AbstractGui {
    private static final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");
    private final DailyRewardsModule dailyRewardsModule;
    private final Player player;
    private final GuiFormat.GuiTemplate guiTemplate;
    private final Inventory inventory;

    public DailyRewardsGui(DailyRewardsModule dailyRewardsModule, Player player) {
        this.dailyRewardsModule = dailyRewardsModule;
        this.player = player;

        GuiFormat guiFormat = dailyRewardsModule.getGuiFormat();
        this.guiTemplate = guiFormat.getTemplate();
        this.inventory = Bukkit.createInventory(null, (guiTemplate.getRowCount() * 9), ChatColorHandler.translateAlternateColorCodes(dailyRewardsModule.getGuiFormat().getTitle()));
    }

    @Override
    public void recalculateContents() {
        inventory.clear();

        // Gets RewardUser
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);

        // Checks if the streak mode config option is enabled
        if (ActivityRewarder.getConfigManager().isStreakModeEnabled()) {
            // Resets RewardUser to Day 1 if a day has been missed
            if (rewardUser.getLastDate().isBefore(LocalDate.now().minusDays(1))) {
                rewardUser.resetDays();
            }
        }

        // The current day number being shown to the user
        int currDayNum = rewardUser.getDayNum();

        // The day number that the user is technically on
        int actualDayNum = rewardUser.getActualDayNum();

        // Checks if the reward has been collected today
        boolean collectedToday = rewardUser.hasCollectedToday();
        if (collectedToday) {
            currDayNum -= 1;
        }

        int dayIndex;
        SimpleDate dateIndex;
        switch(dailyRewardsModule.getScrollType()) {
            case GRID -> {
                int rewardDisplaySize = guiTemplate.countChar('R');

                int endDay = rewardDisplaySize;
                while (currDayNum > endDay) {
                    endDay += rewardDisplaySize;
                }

                dayIndex = endDay - (rewardDisplaySize - 1);

                int diff = dayIndex - currDayNum;
                dateIndex = SimpleDate.now();
                dateIndex.addDays(diff);
            }
            case MONTH -> {
                SimpleDate today = SimpleDate.now();
                dateIndex = new SimpleDate(1, today.getMonth(), today.getYear());
                dayIndex = currDayNum - (today.getDay() - 1);
            }
            default -> {
                dayIndex = currDayNum;
                dateIndex = SimpleDate.now();
            }
        }

        List<Integer> upcomingRewardSlots = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            char slotChar = guiTemplate.getCharAt(slot);

            switch (slotChar) {
                case 'R' -> {
                    // Get the day's reward for the current slot
                    DailyRewardCollection reward = dailyRewardsModule.getRewardDay(dateIndex, dayIndex).getHighestPriorityRewardCollection();
                    // TODO: Store list of collected days in RewardUser to check for collected vs missed days

                    String itemTemplate = (dayIndex == currDayNum && collectedToday) ? "collected-reward" : "default-reward";

                    SimpleItemStack displayItem = SimpleItemStack.overwrite(reward.getDisplayItem(), ActivityRewarder.getConfigManager().getCategoryTemplate(reward.getCategory()));
                    displayItem = SimpleItemStack.overwrite(displayItem, ActivityRewarder.getConfigManager().getItemTemplate(itemTemplate));

                    if (displayItem.hasDisplayName()) {
                        displayItem.setDisplayName(ChatColorHandler.translateAlternateColorCodes(displayItem.getDisplayName()
                            .replaceAll("%day%", String.valueOf(dayIndex))
                            .replaceAll("%month_day%", String.valueOf(dateIndex.getDay())),
                            player));
                    }
                    displayItem.setLore(ChatColorHandler.translateAlternateColorCodes(displayItem.getLore(), player));

                    ItemStack itemStack;
                    try {
                        itemStack = displayItem.getItemStack(player);
                    } catch(IllegalArgumentException e) {
                        ActivityRewarder.getInstance().getLogger().severe("Failed to display item-template '" + itemTemplate + "' as it does not specify a valid material");
                        itemStack = new ItemStack(Material.STONE);
                    }

                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta != null) {
                        // Changes item data based on if the reward has been collected or not
                        if (dayIndex == currDayNum && collectedToday) {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|unavailable"));
                        } else {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|collectable"));
                        }
                    }

                    if (dailyRewardsModule.showDateAsAmount()) {
                        itemStack.setAmount(dateIndex.getDay());
                    }

                    inventory.setItem(slot, itemStack);

                    dayIndex++;
                    dateIndex.addDays(1);
                }
                case 'U', 'N' -> upcomingRewardSlots.add(slot);
                default -> {
                    SimpleItemStack simpleItemStack = ActivityRewarder.getConfigManager().getItemTemplate(String.valueOf(slotChar));

                    if (!simpleItemStack.hasType()) {
                        simpleItemStack.setType(Material.STONE);
                        ActivityRewarder.getInstance().getLogger().severe("Failed to display custom item-template '" + slotChar + "' as it does not specify a valid material");
                    }
                    simpleItemStack.parseColors(player);

                    inventory.setItem(slot, simpleItemStack.getItemStack(player));
                }
            }
        }

        // Finds next upcoming reward (Excluding rewards shown in the inventory)
        if (upcomingRewardSlots.size() > 0) {
            String upcomingCategory = ActivityRewarder.getConfigManager().getUpcomingCategory();
            int upcomingRewardDay = dailyRewardsModule.findNextRewardFromCategory(dayIndex, upcomingCategory);
            SimpleDate upcomingRewardDate = SimpleDate.now();
            // TODO: Add util method to calculate what date a streak day will be
            upcomingRewardDate.addDays(upcomingRewardDay - rewardUser.getActualDayNum());

            // Adds the upcoming reward to the GUI if it exists
            if (upcomingRewardDay != -1) {
                SimpleItemStack categoryItem = ActivityRewarder.getConfigManager().getCategoryTemplate(upcomingCategory);

                // Get the day's reward for the current slot
                DailyRewardCollection upcomingReward = dailyRewardsModule.getRewardDay(upcomingRewardDate, upcomingRewardDay).getHighestPriorityRewardCollection();
                SimpleItemStack simpleItemStack = SimpleItemStack.overwrite(categoryItem, ActivityRewarder.getConfigManager().getItemTemplate("upcoming-reward"));
                simpleItemStack = SimpleItemStack.overwrite(simpleItemStack, upcomingReward.getDisplayItem());

                if (simpleItemStack.hasDisplayName()) {
                    simpleItemStack.setDisplayName(ChatColorHandler.translateAlternateColorCodes(simpleItemStack.getDisplayName()
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

                upcomingRewardSlots.forEach((slot) -> inventory.setItem(slot, itemStack));
            }
        }
    }

    @Override
    public void openInventory() {
        recalculateContents();
        player.openInventory(inventory);
        InventoryHandler.putInventory(player.getUniqueId(), this);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true);
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null || clickedInv.getType() != InventoryType.CHEST) {
            return;
        }

        // Gets clicked item and checks if it exists
        ItemStack currItem = event.getCurrentItem();
        if (currItem == null) {
            return;
        }
        // Gets clicked item's meta and checks if it exists
        ItemMeta currItemMeta = currItem.getItemMeta();
        if (currItemMeta == null) {
            return;
        }
        // Gets persistent data of clicked item and checks if it exists
        String persistentData = currItemMeta.getPersistentDataContainer().get(activityRewarderKey, PersistentDataType.STRING);
        if (persistentData == null) {
            return;
        }
        // Formats data into an array
        String[] persistentDataArr = persistentData.split(Pattern.quote("|"));

        // Gets current day from data array
        int currDay = Integer.parseInt(persistentDataArr[0]);
        // Checks if reward can be collected
        if (!persistentDataArr[2].equals("collectable")) {
            return;
        }

        ItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), ActivityRewarder.getConfigManager().getItemTemplate("collected-reward")).getItemStack(player);
        ItemMeta collectedItemMeta = collectedItem.getItemMeta();
        if (collectedItemMeta != null) {
            collectedItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentDataArr[0] + "|" + persistentDataArr[1] + "|collected"));
            collectedItem.setItemMeta(collectedItemMeta);
        }

        event.getClickedInventory().setItem(event.getSlot(), collectedItem);
        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

        Debugger.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), Debugger.DebugMode.DAILY);
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
        Debugger.sendDebugMessage("Loaded player's daily rewards ", Debugger.DebugMode.DAILY);
        Debugger.sendDebugMessage("Attempting to give rewards to player", Debugger.DebugMode.DAILY);

        RewardDay rewardDay = RewardDay.from(dailyRewardsModule.getStreakRewards(currDay));
        DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();
        if (ActivityRewarder.getConfigManager().shouldStackRewards()) {
            rewardDay.giveAllRewards(player);
        } else {
            priorityReward.giveAll(player);
        }

        Debugger.sendDebugMessage("Successfully gave player rewards", Debugger.DebugMode.DAILY);
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
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public enum ScrollType {
        DAY,
        MONTH,
        GRID
    }
}