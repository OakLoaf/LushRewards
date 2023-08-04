package me.dave.activityrewarder.gui.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.config.DebugMode;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.InventoryHandler;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import me.dave.activityrewarder.rewards.RewardCollection;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RewardsGui extends AbstractGui {
    private final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");
    private final GuiTemplate guiTemplate = ActivityRewarder.configManager.getGuiTemplate();
    private final int slotCount = guiTemplate.getRowCount() * 9;
    private final Inventory inventory = Bukkit.createInventory(null, slotCount, ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiTitle()));
    private final Player player;

    public RewardsGui(Player player) {
        this.player = player;
    }

    @Override
    public void recalculateContents() {
        inventory.clear();

        // Gets RewardUser
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());

        // Checks if the streak mode config option is enabled
        if (ActivityRewarder.configManager.doDaysReset()) {
            // Resets RewardUser to Day 1 if a day has been missed
            if (rewardUser.getLastDate().isBefore(LocalDate.now().minusDays(1))) rewardUser.resetDays();
        }

        // The current day number being shown to the user
        int currDayNum = rewardUser.getDayNum();

        // The day number that the user is technically on
        int actualDayNum = rewardUser.getActualDayNum();

        // Creates the border item
        ItemStack borderItem = ActivityRewarder.configManager.getBorderItem();
        if (borderItem.getType() != Material.AIR) {
            ItemMeta borderMeta = borderItem.getItemMeta();
            borderMeta.setDisplayName("§7");
            borderItem.setItemMeta(borderMeta);
        }

        // Checks if the reward has been collected today
        boolean collectedToday = rewardUser.hasCollectedToday();
        if (collectedToday) currDayNum -= 1;

        int dayIndex = currDayNum;
        List<Integer> upcomingRewardSlots = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            char slotChar = guiTemplate.getCharAt(slot);

            switch (slotChar) {
                case '#' -> inventory.setItem(slot, borderItem);
                case 'R' -> {
                    // Get the day's reward for the current slot
                    RewardCollection reward = ActivityRewarder.configManager.getRewards(dayIndex);
                    ItemStack displayItem = reward.asItem();
                    if (dayIndex == currDayNum && collectedToday) displayItem = ActivityRewarder.configManager.getCollectedItem();

                    ItemMeta displayItemMeta = displayItem.getItemMeta();
                    if (displayItemMeta != null) {
                        displayItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(player, ActivityRewarder.configManager.getGuiItemRedeemableName(dayIndex)));
                        displayItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|unavailable"));

                        // Changes item in first slot based on if the reward has been collected or not
                        if (dayIndex == currDayNum) {
                            if (collectedToday) {
                                displayItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(player, ActivityRewarder.configManager.getGuiItemCollectedName(dayIndex)));
                            } else {
                                displayItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|collectable"));
                                displayItemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                                displayItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                            }
                        }

                        displayItem.setItemMeta(displayItemMeta);
                    }

                    inventory.setItem(slot, displayItem);

                    dayIndex++;
                }
                case 'U', 'N' -> upcomingRewardSlots.add(slot);
            }
        }

        // Finds next large reward (Excluding rewards shown in the inventory)
        if (upcomingRewardSlots.size() > 0) {
            int nextRewardDay = -1;
            if (ActivityRewarder.configManager.showUpcomingReward()) {
                nextRewardDay = ActivityRewarder.configManager.findNextRewardInCategory(dayIndex, "large");
            }

            // Adds the upcoming reward to the GUI if it exists
            if (nextRewardDay != -1) {
                ItemStack upcomingItem = ActivityRewarder.configManager.getCategoryItem("large");

                ItemMeta upcomingItemMeta = upcomingItem.getItemMeta();
                if (upcomingItemMeta != null) {
                    List<String> itemLore = ActivityRewarder.configManager.getUpcomingRewardLore();
                    if (itemLore.isEmpty()) {
                        itemLore.add("§7§o- Next large reward");
                    } else if (itemLore.size() == 1 && itemLore.get(0).equals("")) {
                        // Get the day's reward for the current slot
                        RewardCollection reward = ActivityRewarder.configManager.getRewards(dayIndex);
                        itemLore = reward.lore();
                    }

                    upcomingItemMeta.setLore(ChatColorHandler.translateAlternateColorCodes(player, itemLore));
                    upcomingItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(player, ActivityRewarder.configManager.getGuiItemRedeemableName(nextRewardDay - rewardUser.getDayNumOffset())));
                    upcomingItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, ((nextRewardDay - rewardUser.getDayNumOffset()) + "|" + actualDayNum + "|unavailable"));
                    upcomingItem.setItemMeta(upcomingItemMeta);
                }

                upcomingRewardSlots.forEach((slot) -> inventory.setItem(slot, upcomingItem));
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

        ItemStack collectedItem = ActivityRewarder.configManager.getCollectedItem();
        ItemMeta collectedMeta = collectedItem.getItemMeta();
        collectedMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(player, ActivityRewarder.configManager.getGuiItemCollectedName(currDay)));
        collectedMeta.removeEnchant(Enchantment.DURABILITY);
        collectedMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentDataArr[0] + "|" + persistentDataArr[1] + "|collected"));
        collectedItem.setItemMeta(collectedMeta);
        event.getClickedInventory().setItem(event.getSlot(), collectedItem);
        ActivityRewarder.configManager.sendDebugMessage("Starting reward process for " + player.getName(), DebugMode.ALL);

        ActivityRewarder.configManager.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), DebugMode.DAILY);
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        ActivityRewarder.configManager.sendDebugMessage("Loaded player's daily rewards ", DebugMode.DAILY);
        ActivityRewarder.configManager.sendDebugMessage("Attempting to give rewards to player", DebugMode.DAILY);
        RewardCollection priorityReward = ActivityRewarder.configManager.getRewards(currDay);
        priorityReward.giveRewards(player);
        ActivityRewarder.configManager.sendDebugMessage("Successfully gave player rewards", DebugMode.DAILY);
        ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getRewardMessage());

        ActivityRewarder.configManager.sendDebugMessage("Attempting to send hourly rewards to " + player.getName(), DebugMode.HOURLY);
        RewardCollection hourlyRewards = ActivityRewarder.configManager.getHourlyRewards(player);
        if (hourlyRewards != null) {
            int currPlayTime = rewardUser.getTotalPlayTime();
            ActivityRewarder.configManager.sendDebugMessage("Collected player's total playtime (" + currPlayTime + ")", DebugMode.HOURLY);
            int hoursDiff = currPlayTime - rewardUser.getPlayTime();
            ActivityRewarder.configManager.sendDebugMessage("Calculated difference (" + hoursDiff + ")", DebugMode.HOURLY);
            // Works out how many rewards the user should receive
            int totalRewards = (int) Math.floor(hoursDiff * rewardUser.getHourlyMultiplier());
            ActivityRewarder.configManager.sendDebugMessage("Loaded player's reward count (" + totalRewards + ")", DebugMode.HOURLY);

            ActivityRewarder.configManager.sendDebugMessage("Attempting to give rewards to player", DebugMode.HOURLY);
            for (int i = 0; i < totalRewards; i++) {
                hourlyRewards.giveRewards(player);
                ActivityRewarder.configManager.sendDebugMessage("Successfully gave player a reward", DebugMode.HOURLY);
            }
            if (hoursDiff > 0) ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getBonusMessage().replaceAll("%hours%", String.valueOf(hoursDiff)));
            rewardUser.setPlayTime(currPlayTime);
            ActivityRewarder.configManager.sendDebugMessage("Updated player's stored playtime (" + currPlayTime + ")", DebugMode.HOURLY);
        }

        player.playSound(player.getLocation(), priorityReward.sound(), 1f, 1f);
        rewardUser.incrementDayNum();
        rewardUser.setLastDate(LocalDate.now().toString());
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}