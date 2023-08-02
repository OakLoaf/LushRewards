package me.dave.activityrewarder.notifications;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDate;

public class BukkitNotifications implements NotificationHandler {
    private int counter = 0;

    public void reloadNotifications(int reminderPeriod) {
        counter += 1;
        startNotificationTask(reminderPeriod);
    }

    private void startNotificationTask(int reminderPeriod) {
        int thisNotifNum = counter;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (counter != thisNotifNum) {
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
