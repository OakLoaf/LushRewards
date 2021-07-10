package org.enchantedskies.activityrewarder.events;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
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
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.enchantedskies.activityrewarder.datamanager.RewardUser;
import org.enchantedskies.activityrewarder.rewardtypes.Reward;
import org.geysermc.floodgate.api.FloodgateApi;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RewardGUIEvents implements Listener {
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
        if (currItemMeta == null || !(currItemMeta.hasEnchants() && event.getSlot() == 9)) return;
        ItemStack collectedItem = ActivityRewarder.configManager.getCollectedItem();
        ItemMeta collectedMeta = currItemMeta;
        collectedMeta.setDisplayName(currItemMeta.getDisplayName() + " §6§l- Collected");
        collectedMeta.removeEnchant(Enchantment.DURABILITY);
        collectedItem.setItemMeta(collectedMeta);
        event.getClickedInventory().setItem(9, collectedItem);
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        long actualDayNum = LocalDate.now().toEpochDay() - rewardUser.getStartDate().toEpochDay();
        ActivityRewarder.configManager.getReward((int) actualDayNum % 14 + 1).giveReward(player);
        ConfigurationSection hourlySection = ActivityRewarder.configManager.getConfig().getConfigurationSection("hourly-bonus");
        Reward bonusReward = null;
        for (String perm : hourlySection.getKeys(false)) {
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                bonusReward = ActivityRewarder.configManager.getCustomReward(hourlySection.getConfigurationSection(perm), rewardUser);
            }
        }
        if (bonusReward != null) {
            bonusReward.giveReward(player);
            rewardUser.setPlayTime((int) getTicksToHours(Bukkit.getPlayer(rewardUser.getUUID()).getStatistic(Statistic.PLAY_ONE_MINUTE)));
        }
        if (ActivityRewarder.isFloodgateEnabled()) {
            if (FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(console, "eco give " + player.getName() + " 400");
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        rewardUser.incrementDayNum();
        rewardUser.setLatestDate(LocalDate.now().toString());
    }

    @EventHandler
    public void onInvCloseEvent(InventoryCloseEvent event) {
        guiPlayerSet.remove(event.getPlayer().getUniqueId());
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
