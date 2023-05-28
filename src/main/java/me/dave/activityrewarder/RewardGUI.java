package me.dave.activityrewarder;

import me.dave.activityrewarder.datamanager.GuiTemplate;
import me.dave.activityrewarder.datamanager.RewardUser;
import me.dave.activityrewarder.rewards.RewardsDay;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RewardGUI {
    private final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");

    public void openGUI(Player player) {
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

        GuiTemplate guiTemplate = ActivityRewarder.configManager.getGuiTemplate();
        int rowCount = guiTemplate.getRowCount();
        int slotCount = rowCount * 9;
        Inventory inventory = Bukkit.createInventory(null, slotCount, ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiTitle()));

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
                    RewardsDay reward = ActivityRewarder.configManager.getRewards(dayIndex);
                    ItemStack rewardItem = reward.asItem();
                    if (dayIndex == currDayNum && collectedToday) rewardItem = ActivityRewarder.configManager.getCollectedItem();
                    ItemMeta rewardItemMeta = rewardItem.getItemMeta();

                    rewardItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemRedeemableName(dayIndex)));
                    rewardItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|unavailable"));

                    // Changes item in first slot based on if the reward has been collected or not
                    if (dayIndex == currDayNum) {
                        if (collectedToday) {
                            rewardItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemCollectedName(dayIndex)));
                        } else {
                            rewardItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (dayIndex + "|" + (dayIndex + rewardUser.getDayNumOffset()) + "|collectable"));
                            rewardItemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                            rewardItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        }
                    }

                    rewardItem.setItemMeta(rewardItemMeta);
                    inventory.setItem(slot, rewardItem);

                    dayIndex++;
                }
                case 'U', 'N' -> upcomingRewardSlots.add(slot);
            }
        }

        // Finds next large reward (Excluding rewards shown in the inventory)
        if (upcomingRewardSlots.size() > 0) {
            int nextRewardDay = -1;
            if (ActivityRewarder.configManager.showUpcomingReward()) {
                nextRewardDay = ActivityRewarder.configManager.findNextRewardOfSize(dayIndex, "large");
            }

            // Adds the upcoming reward to the GUI if it exists
            if (nextRewardDay != -1) {
                ItemStack upcomingItem = ActivityRewarder.configManager.getSizeItem("large");
                List<String> itemLore = ActivityRewarder.configManager.getUpcomingRewardLore();
                ItemMeta upcomingMeta = upcomingItem.getItemMeta();

                if (itemLore.isEmpty()) {
                    itemLore.add("§7§o- Next large reward");
                } else if (itemLore.size() == 1 && itemLore.get(0).equals("")) {
                    // Get the day's reward for the current slot
                    RewardsDay reward = ActivityRewarder.configManager.getRewards(dayIndex);
                    itemLore = reward.getLore();
                }

                upcomingMeta.setLore(ChatColorHandler.translateAlternateColorCodes(itemLore));
                upcomingMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemRedeemableName(nextRewardDay - rewardUser.getDayNumOffset())));
                upcomingMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, ((nextRewardDay - rewardUser.getDayNumOffset()) + "|" + actualDayNum + "|unavailable"));
                upcomingItem.setItemMeta(upcomingMeta);

                upcomingRewardSlots.forEach((slot) -> inventory.setItem(slot, upcomingItem));
            }
        }

        player.openInventory(inventory);
    }
}
