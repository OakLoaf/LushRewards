package me.dave.lushrewards.notifications;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsModule;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.libraries.paperlib.morepaperlib.scheduling.ScheduledTask;
import me.dave.platyutils.module.Module;
import org.bukkit.Bukkit;

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

        this.notificationTask = PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(() -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
                for (Module module : LushRewards.getInstance().getModules()) {
                    if (module instanceof DailyRewardsModule && rewardUser.getModuleData(module.getId()) instanceof DailyRewardsModule.UserData userData && !userData.hasCollectedToday()) {
                        ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("reminder"));
                        player.playSound(player.getLocation(), LushRewards.getInstance().getConfigManager().getReminderSound(), 1f, 1.5f);
                        return;
                    }
                }
            });
        }, Duration.of(Math.round((double) reminderPeriodMs / 3), ChronoUnit.MILLIS), Duration.of(reminderPeriodMs, ChronoUnit.MILLIS));
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
