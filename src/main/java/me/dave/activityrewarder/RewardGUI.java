package me.dave.activityrewarder;

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
        Material borderMaterial = ActivityRewarder.configManager.getBorderMaterial();
        ItemStack borderItem = new ItemStack(borderMaterial);
        if (borderMaterial != Material.AIR) {
            ItemMeta borderMeta = borderItem.getItemMeta();
            borderMeta.setDisplayName("§7");
            borderItem.setItemMeta(borderMeta);
        }


        Inventory inventory = Bukkit.createInventory(null, 27, ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiTitle()));
        for (int i = 0; i < 27; i++) inventory.setItem(i, borderItem);

        // Checks if the reward has been collected today
        boolean collectedToday = LocalDate.now().equals(rewardUser.getLastDate());
        if (collectedToday) currDayNum -= 1;

        // Finds next large reward (Excluding rewards shown in the inventory)
        int nextRewardDay = ActivityRewarder.configManager.findNextRewardOfSize((actualDayNum + 6), "large");

        // Adds the upcoming reward to the GUI
        if (nextRewardDay != -1) {
            ItemStack upcomingItem = ActivityRewarder.configManager.getSizeItem("large");
            List<String> itemLore = new ArrayList<>();
            ItemMeta upcomingMeta = upcomingItem.getItemMeta();
            itemLore.add("§7§o- Next large reward");
            upcomingMeta.setLore(itemLore);
            upcomingMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemRedeemableName(nextRewardDay - rewardUser.getDayNumOffset())));
            upcomingMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, ((nextRewardDay - rewardUser.getDayNumOffset()) + "|" + actualDayNum + "|unavailable"));
            upcomingItem.setItemMeta(upcomingMeta);
            inventory.setItem(17, upcomingItem);
        }

        for (int i = 9; i < 16; i++) {
            // Get the current slot's reward
            RewardsDay reward = ActivityRewarder.configManager.getRewards(actualDayNum + (i - 9));

            // Get the current reward's size item
            String size = reward.getSize();
            ItemStack rewardItem = ActivityRewarder.configManager.getSizeItem(size);
            ItemMeta rewardItemMeta = rewardItem.getItemMeta();
            List<String> itemLore = new ArrayList<>();
            itemLore.add("§7§o- " + makeFriendly(size) + " reward");
            rewardItemMeta.setLore(itemLore);
            rewardItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemRedeemableName(currDayNum + (i - 9))));
            rewardItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (currDayNum + "|" + actualDayNum + "|unavailable"));

            // Changes item in first slot based on if the reward has been collected or not
            if (i == 9) {
                if (collectedToday) {
                    rewardItem = ActivityRewarder.configManager.getCollectedItem();
                    rewardItemMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemCollectedName(currDayNum)));
                }
                else {
                    rewardItemMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (currDayNum + "|" + actualDayNum + "|collectable"));
                    rewardItemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                    rewardItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }

            rewardItem.setItemMeta(rewardItemMeta);
            if (nextRewardDay == -1) inventory.setItem(i + 1, rewardItem);
            else inventory.setItem(i, rewardItem);
        }

        player.openInventory(inventory);
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
