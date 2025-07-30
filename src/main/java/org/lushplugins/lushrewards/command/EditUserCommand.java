package org.lushplugins.lushrewards.command;

import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsUserData;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.time.LocalDate;

@SuppressWarnings("unused")
@Command("rewards edit-user <player>")
public class EditUserCommand {

    @Subcommand("playtime set")
    @CommandPermission("lushrewards.edituser.playtime.set")
    public void setPlaytime(RewardUser user, int playtime) {
        for (PlaytimeRewardsUserData userData : user.getAllModuleUserData(PlaytimeRewardsUserData.class)) {
            if (userData.getLastCollectedPlaytime() > playtime) {
                userData.setLastCollectedPlaytime(playtime);
            }

            if (userData.getPreviousDayEndPlaytime() > playtime) {
                userData.setPreviousDayEndPlaytime(playtime);
            }
        }

        // The below method also saves the user, so we rely on this
        user.setMinutesPlayed(playtime);
    }

    @Subcommand("playtime reset")
    @CommandPermission("lushrewards.edituser.playtime.reset")
    public void resetPlaytime(RewardUser user) {
        this.setPlaytime(user, 0);
    }

    @Subcommand("reset")
    @CommandPermission("lushrewards.edituser.reset")
    public void reset(RewardUser user) {
        // TODO: Implement and require additional `confirm` option
    }

    // TODO: Migrate to Orphan command
    public static class DailyRewards {

        @Subcommand("days set")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setDayNum(DailyRewardsModule module, DailyRewardsUserData user, int dayNum) {
            user.setDayNum(dayNum);
            user.setLastCollectedDate(DailyRewardsUserData.NEVER_COLLECTED);
            module.saveUserData(user);
        }

        @Subcommand("days reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetDayNum(DailyRewardsModule module, DailyRewardsUserData user) {
            user.setDayNum(0);
            user.setLastCollectedDate(DailyRewardsUserData.NEVER_COLLECTED);
            module.saveUserData(user);
        }

        @Subcommand("streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setStreak(DailyRewardsModule module, DailyRewardsUserData user, int streak) {
            user.setStreak(streak);
            module.saveUserData(user);
        }

        @Subcommand("streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetStreak(DailyRewardsModule module, DailyRewardsUserData user) {
            user.setStreak(0);
            module.saveUserData(user);
        }

        @Subcommand("highest-streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setHighestStreak(DailyRewardsModule module, DailyRewardsUserData user, int highestStreak) {
            user.setHighestStreak(highestStreak);
            module.saveUserData(user);
        }

        @Subcommand("highest-streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetHighestStreak(DailyRewardsModule module, DailyRewardsUserData user) {
            user.setHighestStreak(0);
            module.saveUserData(user);
        }
    }

    // TODO: Migrate to Orphan command
    public static class PlaytimeRewards {

        @Subcommand("last-collected-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.lastcollectedplaytime.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setLastCollectedPlaytime(PlaytimeRewardsModule module, PlaytimeRewardsUserData user, int lastCollectedPlaytime) {
            user.setLastCollectedPlaytime(lastCollectedPlaytime);
            module.saveUserData(user);
        }

        @Subcommand("start-date set")
        @CommandPermission("lushrewards.edituser.playtimerewards.startdate.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setStartDate(PlaytimeRewardsModule module, PlaytimeRewardsUserData user, LocalDate startDate) {
            user.setStartDate(startDate);
            module.saveUserData(user);
        }

        @Subcommand("previous-day-end-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.previousdayendplaytime.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setPreviousDayEndPlaytime(PlaytimeRewardsModule module, PlaytimeRewardsUserData user, int previousDayEndPlaytime) {
            user.setPreviousDayEndPlaytime(previousDayEndPlaytime);
            module.saveUserData(user);
        }
    }
}
