package org.enchantedskies.activityrewarder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.enchantedskies.activityrewarder.datamanager.RewardUser;
import org.enchantedskies.activityrewarder.rewardtypes.Reward;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RewardGUI {

    public RewardGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, "§8§lDaily Rewards");
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        int displayDayNum = rewardUser.getDayNum();
        LocalDate startDate = rewardUser.getStartDate();
        long actualDayNum = LocalDate.now().toEpochDay() - startDate.toEpochDay();
        ItemStack borderItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderItem.getItemMeta();
        borderMeta.setDisplayName("§7");
        borderItem.setItemMeta(borderMeta);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, borderItem);
        }
        boolean collectedToday = LocalDate.now().equals(rewardUser.getLastDate());
        if (collectedToday) displayDayNum -= 1;
        for (int i = 9; i < 16; i++) {
            Reward currReward = ActivityRewarder.configManager.getReward((int) actualDayNum % 14 + 1);
            String rewardSize = currReward.getSize();
            ItemStack thisItem = ActivityRewarder.configManager.getSizeItem(rewardSize);
            ItemMeta thisItemMeta = thisItem.getItemMeta();
            List<String> itemLore = new ArrayList<>();
            itemLore.add("§7§o- " + makeFriendly(rewardSize) + " reward");
            thisItemMeta.setLore(itemLore);
            thisItemMeta.setDisplayName("§6§lDay " + displayDayNum);
            if (i == 9) {
                if (collectedToday) {
                    thisItem = ActivityRewarder.configManager.getCollectedItem();
                    thisItemMeta.setDisplayName(thisItemMeta.getDisplayName() + " §6§l- Collected");
                }
                else {
                    thisItemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                    thisItemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            thisItem.setItemMeta(thisItemMeta);
            inventory.setItem(i, thisItem);
            displayDayNum += 1;
            actualDayNum += 1;
        }
        for (int i = 0; i < ActivityRewarder.configManager.getLoopLength(); i++) {
            Reward currReward = ActivityRewarder.configManager.getReward((int) actualDayNum % 14 + 1);
            if (currReward.getSize().equalsIgnoreCase("large")) break;
            displayDayNum += 1;
            actualDayNum += 1;
        }
        ItemStack upcomingItem = new ItemStack(Material.GOLD_BLOCK);
        List<String> itemLore = new ArrayList<>();
        ItemMeta upcomingMeta = upcomingItem.getItemMeta();
        itemLore.add("§7§o- Next large reward");
        upcomingMeta.setLore(itemLore);
        upcomingMeta.setDisplayName("§6§lDay " + displayDayNum);
        upcomingItem.setItemMeta(upcomingMeta);
        inventory.setItem(17, upcomingItem);
        player.openInventory(inventory);
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
