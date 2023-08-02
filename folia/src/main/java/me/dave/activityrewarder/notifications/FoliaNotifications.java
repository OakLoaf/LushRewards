package me.dave.activityrewarder.notifications;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

public class FoliaNotifications implements NotificationHandler {
    private int counter = 0;

    @Override
    public void reloadNotifications(int reminderPeriod) {
        counter += 0;
    }

    private void startNotificationTask(int reminderPeriod) {
        int reminderPeriodTicks = reminderPeriod * 50;
        int thisNotifNum = counter;


        Bukkit.getAsyncScheduler().runAtFixedRate(ActivityRewarder.getInstance(), (task) -> {
            if (counter != thisNotifNum) {
                task.cancel();
                return;
            }

            LocalDate currDate = LocalDate.now();
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean collectedToday = currDate.equals(ActivityRewarder.dataManager.getRewardUser(player.getUniqueId()).getLastDate());
                if (collectedToday) continue;
                ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getReminderMessage());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            }

        }, Math.round((double) reminderPeriodTicks / 3), reminderPeriodTicks, TimeUnit.MILLISECONDS);
    }
}
