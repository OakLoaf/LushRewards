package me.dave.activityrewarder.gui.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.InventoryHandler;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.HourlyRewardCollection;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
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

public class RewardsGui extends AbstractGui {
    private final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");
    private final GuiTemplate guiTemplate = ActivityRewarder.getConfigManager().getGuiFormat().template();
    private final int slotCount = guiTemplate.getRowCount() * 9;
    private final Inventory inventory = Bukkit.createInventory(null, slotCount, ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.getConfigManager().getGuiFormat().title()));
    private final Player player;

    public RewardsGui(Player player) {
        this.player = player;
    }

    @Override
    public void recalculateContents() {
        inventory.clear();

        // Gets RewardUser
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player.getUniqueId());

        // Checks if the streak mode config option is enabled
        if (ActivityRewarder.getConfigManager().doDaysReset()) {
            // Resets RewardUser to Day 1 if a day has been missed
            if (rewardUser.getLastDate().isBefore(LocalDate.now().minusDays(1))) rewardUser.resetDays();
        }

        // The current day number being shown to the user
        int currDayNum = rewardUser.getDayNum();

        // The day number that the user is technically on
        int actualDayNum = rewardUser.getActualDayNum();

        // Checks if the reward has been collected today
        boolean collectedToday = rewardUser.hasCollectedToday();
        if (collectedToday) currDayNum -= 1;

        int dayIndex = currDayNum;
        List<Integer> upcomingRewardSlots = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            char slotChar = guiTemplate.getCharAt(slot);

            switch (slotChar) {
                case 'R' -> {
                    // Get the day's reward for the current slot
                    DailyRewardCollection reward = ActivityRewarder.getRewardManager().getRewards(dayIndex).getHighestPriorityRewards();
                    // TODO: Store list of collected days in RewardUser to check for collected vs missed days
                    SimpleItemStack displayItem = (dayIndex == currDayNum && collectedToday) ? SimpleItemStack.overwrite(reward.getDisplayItem(), ActivityRewarder.getConfigManager().getItemTemplate("collected-reward")) : reward.getDisplayItem();

                    if (displayItem.hasDisplayName()) displayItem.setDisplayName(ChatColorHandler.translateAlternateColorCodes(displayItem.getDisplayName().replaceAll("%day%", String.valueOf(dayIndex)), player));
                    displayItem.setLore(ChatColorHandler.translateAlternateColorCodes(displayItem.getLore(), player));

                    ItemStack itemStack = displayItem.getItemStack();
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta != null) {
                        // Changes item data based on if the reward has been collected or not
                        if (dayIndex == currDayNum && collectedToday) {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|unavailable"));
                        }
                        else {
                            itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|collectable"));
                        }
                    }

                    inventory.setItem(slot, itemStack);

                    dayIndex++;
                }
                case 'U', 'N' -> upcomingRewardSlots.add(slot);
                default -> inventory.setItem(slot, ActivityRewarder.getConfigManager().getItemTemplate(String.valueOf(slotChar)).getItemStack());
            }
        }

        // Finds next large reward (Excluding rewards shown in the inventory)
        if (upcomingRewardSlots.size() > 0) {
            // TODO: Add config option for upcoming category name
            int nextRewardDay = -1;
            if (ActivityRewarder.getConfigManager().getUpcomingRewardFormat().enabled()) {
                nextRewardDay = ActivityRewarder.getRewardManager().findNextRewardFromCategory(dayIndex, "large");
            }

            // Adds the upcoming reward to the GUI if it exists
            if (nextRewardDay != -1) {
                SimpleItemStack categoryItem = ActivityRewarder.getConfigManager().getCategoryItem("large");

                // Get the day's reward for the current slot
                DailyRewardCollection upcomingReward = ActivityRewarder.getRewardManager().getRewards(dayIndex).getHighestPriorityRewards();
                SimpleItemStack simpleItemStack = SimpleItemStack.overwrite(categoryItem, ActivityRewarder.getConfigManager().getItemTemplate("upcoming-reward"));
                simpleItemStack = SimpleItemStack.overwrite(simpleItemStack, upcomingReward.getDisplayItem());

                if (simpleItemStack.hasDisplayName()) simpleItemStack.setDisplayName(ChatColorHandler.translateAlternateColorCodes(simpleItemStack.getDisplayName().replaceAll("%day%", String.valueOf(nextRewardDay - rewardUser.getDayNumOffset())), player));
                simpleItemStack.setLore(ChatColorHandler.translateAlternateColorCodes(simpleItemStack.getLore(), player));

                ItemStack itemStack = simpleItemStack.getItemStack();
                ItemMeta itemMeta = itemStack.getItemMeta();
                if (itemMeta != null) {
                    itemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, ((nextRewardDay - rewardUser.getDayNumOffset()) + "|" + actualDayNum + "|unavailable"));
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
        if (clickedInv == null || clickedInv.getType() != InventoryType.CHEST) return;

        // Gets clicked item and checks if it exists
        ItemStack currItem = event.getCurrentItem();
        if (currItem == null) return;
        // Gets clicked item's meta and checks if it exists
        ItemMeta currItemMeta = currItem.getItemMeta();
        if (currItemMeta == null) return;
        // Gets persistent data of clicked item and checks if it exists
        String persistentData = currItemMeta.getPersistentDataContainer().get(activityRewarderKey, PersistentDataType.STRING);
        if (persistentData == null) return;
        // Formats data into an array
        String[] persistentDataArr = persistentData.split(Pattern.quote("|"));

        // Gets current day from data array
        int currDay = Integer.parseInt(persistentDataArr[0]);
        // Checks if reward can be collected
        if (!persistentDataArr[2].equals("collectable")) return;

        ItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), ActivityRewarder.getConfigManager().getItemTemplate("collected-reward")).getItemStack();
        ItemMeta collectedItemMeta = collectedItem.getItemMeta();
        if (collectedItemMeta != null) {
            collectedItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentDataArr[0] + "|" + persistentDataArr[1] + "|collected"));
            collectedItem.setItemMeta(collectedItemMeta);
        }

        event.getClickedInventory().setItem(event.getSlot(), collectedItem);
        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

        Debugger.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), Debugger.DebugMode.DAILY);
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player.getUniqueId());
        Debugger.sendDebugMessage("Loaded player's daily rewards ", Debugger.DebugMode.DAILY);
        Debugger.sendDebugMessage("Attempting to give rewards to player", Debugger.DebugMode.DAILY);

        // TODO: Add option to give all rewards
        DailyRewardCollection priorityReward = ActivityRewarder.getRewardManager().getRewards(currDay).getHighestPriorityRewards();
        priorityReward.getRewards().forEach((reward) -> reward.giveTo(player));
        Debugger.sendDebugMessage("Successfully gave player rewards", Debugger.DebugMode.DAILY);
        ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("daily-reward-given"));

        Debugger.sendDebugMessage("Attempting to send hourly rewards to " + player.getName(), Debugger.DebugMode.HOURLY);
        HourlyRewardCollection hourlyRewardData = ActivityRewarder.getRewardManager().getHourlyRewards(player);
        if (hourlyRewardData != null) {
            int currPlayTime = rewardUser.getTotalPlayTime();
            Debugger.sendDebugMessage("Collected player's total playtime (" + currPlayTime + ")", Debugger.DebugMode.HOURLY);
            int hoursDiff = currPlayTime - rewardUser.getPlayTime();
            Debugger.sendDebugMessage("Calculated difference (" + hoursDiff + ")", Debugger.DebugMode.HOURLY);
            // Works out how many rewards the user should receive
            int totalRewards = (int) Math.floor(hoursDiff * hourlyRewardData.getMultiplier());
            Debugger.sendDebugMessage("Loaded player's reward count (" + totalRewards + ")", Debugger.DebugMode.HOURLY);

            Debugger.sendDebugMessage("Attempting to give rewards to player", Debugger.DebugMode.HOURLY);
            for (int i = 0; i < totalRewards; i++) {
                hourlyRewardData.getRewards().forEach((reward) -> reward.giveTo(player));
                Debugger.sendDebugMessage("Successfully gave player a reward", Debugger.DebugMode.HOURLY);
            }
            if (hoursDiff > 0) ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("hourly-reward-given").replaceAll("%hours%", String.valueOf(hoursDiff)));
            rewardUser.setPlayTime(currPlayTime);
            Debugger.sendDebugMessage("Updated player's stored playtime (" + currPlayTime + ")", Debugger.DebugMode.HOURLY);
        }

        player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
        rewardUser.incrementDayNum();
        rewardUser.setLastDate(LocalDate.now().toString());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}