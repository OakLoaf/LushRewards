package me.dave.activityrewarder.notifications;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class NotificationHandler {
    private int counter = 0;


    public void reloadNotifications(int reminderPeriod) {
        counter += 1;
        startNotificationTask(reminderPeriod);
    }

    private void startNotificationTask(int reminderPeriod) {
        int reminderPeriodMs = reminderPeriod * 50;
        int thisNotifNum = counter;


        ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate((task) -> {
            if (counter != thisNotifNum) {
                task.cancel();
                return;
            }

            SimpleDate today = SimpleDate.now();
            for (Player player : Bukkit.getOnlinePlayers()) {
                boolean collectedToday = today.equals(ActivityRewarder.getDataManager().getRewardUser(player).getLastDate());
                if (collectedToday) {
                    continue;
                }
                
                ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("reminder"));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            }

        }, Duration.of(Math.round((double) reminderPeriodMs / 3), ChronoUnit.MILLIS), Duration.of(reminderPeriodMs, ChronoUnit.MILLIS));
    }
}
