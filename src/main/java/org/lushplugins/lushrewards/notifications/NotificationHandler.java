package org.lushplugins.lushrewards.notifications;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.bukkit.Bukkit;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class NotificationHandler {
    private ScheduledTask notificationTask;

    public void startNotificationTask(int reminderPeriod) {
        if (reminderPeriod <= 0) {
            return;
        }

        int reminderPeriodMs = reminderPeriod * 50;

        if (this.notificationTask != null) {
            notificationTask.cancel();
        }

        this.notificationTask = LushRewards.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(() -> Bukkit.getOnlinePlayers().forEach(player -> {
            if (LushRewards.getInstance().getEnabledRewardModules().stream().anyMatch(rewardModule -> rewardModule.shouldNotify() && rewardModule.hasClaimableRewards(player) && player.hasPermission("lushrewards.use." + rewardModule.getId()))) {
                ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("reminder"));
                player.playSound(player.getLocation(), LushRewards.getInstance().getConfigManager().getReminderSound(), 1f, 1.5f);
            }
        }), Duration.of(Math.round((double) reminderPeriodMs / 3), ChronoUnit.MILLIS), Duration.of(reminderPeriodMs, ChronoUnit.MILLIS));
    }

    public void stopNotificationTask() {
        if (notificationTask != null) {
            this.notificationTask.cancel();
            this.notificationTask = null;
        }
    }

    public void reloadNotifications() {
        stopNotificationTask();
        startNotificationTask(LushRewards.getInstance().getConfigManager().getReminderPeriod());
    }
}
