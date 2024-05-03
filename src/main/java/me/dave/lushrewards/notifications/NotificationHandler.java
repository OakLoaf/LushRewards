package me.dave.lushrewards.notifications;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;
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
            for (RewardModule module : LushRewards.getInstance().getEnabledRewardModules()) {
                // TODO: Add config option to modules to enable notifications for modules (Default true for DailyRewards and false for others)
                if (module.hasClaimableRewards(player)) {
                    ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("reminder"));
                    player.playSound(player.getLocation(), LushRewards.getInstance().getConfigManager().getReminderSound(), 1f, 1.5f);
                    return;
                }
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
