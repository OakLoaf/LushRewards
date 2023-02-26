package me.dave.activityrewarder.events;

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

        ItemStack currItem = event.getCurrentItem();
        if (currItem == null) return;
        ItemMeta currItemMeta = currItem.getItemMeta();
        if (currItemMeta == null) return;
        String persistentData = currItemMeta.getPersistentDataContainer().get(activityRewarderKey, PersistentDataType.STRING);
        if (persistentData == null) return;
        String[] persistentDataArr = persistentData.split(Pattern.quote("|"));

        int currDay = Integer.parseInt(persistentDataArr[0]);
        if (!persistentDataArr[2].equals("collectable")) return;

        ItemStack collectedItem = ActivityRewarder.configManager.getCollectedItem();
        ItemMeta collectedMeta = collectedItem.getItemMeta();
        collectedMeta.setDisplayName(ChatColorHandler.translateAlternateColorCodes(ActivityRewarder.configManager.getGuiItemCollectedName(currDay)));
        collectedMeta.removeEnchant(Enchantment.DURABILITY);
        collectedMeta.getPersistentDataContainer().set(activityRewarderKey, PersistentDataType.STRING, (persistentDataArr[0] + "|" + persistentDataArr[1] + "|collected"));
        collectedItem.setItemMeta(collectedMeta);
        event.getClickedInventory().setItem(event.getSlot(), collectedItem);

        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        int actualDayNum = rewardUser.getActualDayNum();
        ActivityRewarder.configManager.getRewards(actualDayNum % ActivityRewarder.configManager.getLoopLength()).giveRewards(player);
        ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getRewardMessage());

        RewardsDay hourlyRewards = ActivityRewarder.configManager.getHourlyRewards(player);
        if (hourlyRewards != null) {
            int currPlayTime = rewardUser.getTotalPlayTime();
            int hoursDiff = currPlayTime - rewardUser.getPlayTime();
            // Works out how many rewards the user should receive
            int totalRewards = (int) Math.floor(hoursDiff * rewardUser.getHourlyMultiplier());

            for (int i = 0; i < totalRewards; i++) {
                hourlyRewards.giveRewards(player);
            }
            if (hoursDiff > 0) ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getBonusMessage().replaceAll("%hours%", String.valueOf(hoursDiff)));
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
}
