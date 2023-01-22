package me.dave.activityrewarder;

import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;

public class NotificationHandler {
    private int notifCounter = 0;

    public void reloadNotifications(int reminderPeriod) {
        notifCounter += 1;
        notifyPlayers(reminderPeriod);
    }

    private void notifyPlayers(int reminderPeriod) {
        int thisNotifNum = notifCounter;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (notifCounter != thisNotifNum) {
                    cancel();
                    return;
                }

                LocalDate currDate = LocalDate.now();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    boolean collectedToday = currDate.equals(ActivityRewarder.dataManager.getRewardUser(player.getUniqueId()).getLastDate());
                    if (collectedToday) continue;
                    ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getReminderMessage());
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                }
            }
        }.runTaskTimerAsynchronously(ActivityRewarder.getInstance(), Math.round((double) reminderPeriod / 3), reminderPeriod);
    }
}
