package me.dave.activityrewarder.events;

import me.dave.activityrewarder.datamanager.DebugMode;
import me.dave.activityrewarder.rewards.RewardsDay;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.datamanager.RewardUser;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;

public class RewardGUIEvents implements Listener {
    private final NamespacedKey activityRewarderKey = new NamespacedKey(ActivityRewarder.getInstance(), "ActivityRewarder");
    private final HashSet<UUID> guiPlayerSet;

    public RewardGUIEvents(HashSet<UUID> guiPlayerSet) {
        this.guiPlayerSet = guiPlayerSet;
    }

    @EventHandler
    public void onInvClickEvent(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!guiPlayerSet.contains(player.getUniqueId())) return;
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
        collectedMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemCollectedName(currDay)));
        collectedMeta.removeEnchant(Enchantment.DURABILITY);
        collectedMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentDataArr[0] + "|" + persistentDataArr[1] + "|collected"));
        collectedItem.setItemMeta(collectedMeta);
        event.getClickedInventory().setItem(event.getSlot(), collectedItem);
        ActivityRewarder.configManager.sendDebugMessage("Starting reward process for " + player.getName(), DebugMode.ALL);

        ActivityRewarder.configManager.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), DebugMode.DAILY);
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        ActivityRewarder.configManager.sendDebugMessage("Loaded player's daily rewards ", DebugMode.DAILY);
        ActivityRewarder.configManager.sendDebugMessage("Attempting to give rewards to player", DebugMode.DAILY);
        ActivityRewarder.configManager.getRewards(currDay).giveRewards(player);
        ActivityRewarder.configManager.sendDebugMessage("Successfully gave player rewards", DebugMode.DAILY);
        ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getRewardMessage());

        ActivityRewarder.configManager.sendDebugMessage("Attempting to send hourly rewards to " + player.getName(), DebugMode.HOURLY);
        RewardsDay hourlyRewards = ActivityRewarder.configManager.getHourlyRewards(player);
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

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        rewardUser.incrementDayNum();
        rewardUser.setLastDate(LocalDate.now().toString());
    }

    @EventHandler
    public void onInvCloseEvent(InventoryCloseEvent event) {
        guiPlayerSet.remove(event.getPlayer().getUniqueId());
    }
}
