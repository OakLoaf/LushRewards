package me.dave.activityrewarder.events;

import me.dave.activityrewarder.rewards.RewardsDay;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Statistic;
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
import java.util.concurrent.TimeUnit;
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

        ItemStack currItem = event.getCurrentItem();
        if (currItem == null) return;
        ItemMeta currItemMeta = currItem.getItemMeta();
        if (currItemMeta == null) return;
        String[] persistentData = currItemMeta.getPersistentDataContainer().get(activityRewarderKey, PersistentDataType.STRING).split(Pattern.quote("|"));
        int currDay = Integer.parseInt(persistentData[0]);
        if (!persistentData[2].equals("collectable")) return;

        ItemStack collectedItem = ActivityRewarder.configManager.getCollectedItem();
        ItemMeta collectedMeta = collectedItem.getItemMeta();
        collectedMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemCollectedName(currDay)));
        collectedMeta.removeEnchant(Enchantment.DURABILITY);
        collectedMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentData[0] + "|" + persistentData[1] + "|collected"));
        collectedItem.setItemMeta(collectedMeta);
        event.getClickedInventory().setItem(event.getSlot(), collectedItem);

        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        long actualDayNum = LocalDate.now().toEpochDay() - rewardUser.getStartDate().toEpochDay();
        ActivityRewarder.configManager.getRewards((int) actualDayNum % 14 + 1).giveRewards(player);

        RewardsDay hourlyRewards = ActivityRewarder.configManager.getHourlyRewards(player);
        if (hourlyRewards != null) {
            // Gets the player's current play time
            int currPlayTime = getTicksToHours(Bukkit.getPlayer(rewardUser.getUUID()).getStatistic(Statistic.PLAY_ONE_MINUTE));
            // Finds the difference between the
            int hoursDiff = currPlayTime - rewardUser.getPlayTime();
            // Works out how many rewards the user should receive
            int totalRewards = (int) Math.floor(hoursDiff * hourlyRewards.getMultiplier());

            for (int i = 0; i < totalRewards; i++) {
                hourlyRewards.giveRewards(player);
            }
            rewardUser.setPlayTime(currPlayTime);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        rewardUser.incrementDayNum();
        rewardUser.setLastDate(LocalDate.now().toString());
    }

    @EventHandler
    public void onInvCloseEvent(InventoryCloseEvent event) {
        guiPlayerSet.remove(event.getPlayer().getUniqueId());
    }

    private int getTicksToHours(long ticksPlayed) {
        return (int) TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
