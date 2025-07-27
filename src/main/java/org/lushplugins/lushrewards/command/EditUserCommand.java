package org.lushplugins.lushrewards.command;

import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
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
        user.setMinutesPlayed(playtime);
    }

    @Subcommand("playtime reset")
    @CommandPermission("lushrewards.edituser.playtime.reset")
    public void resetPlaytime(RewardUser user) {
        user.setMinutesPlayed(0);
    }

    @Subcommand("reset")
    @CommandPermission("lushrewards.edituser.reset")
    public void reset(RewardUser user) {
        // TODO: Implement and require additional `confirm` option
    }

    public static class DailyRewards {

        @Subcommand("days set")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setDayNum(DailyRewardsModule module, DailyRewardsModule.UserData user, int dayNum) {
            user.setDayNum(dayNum);
            user.setLastCollectedDate(DailyRewardsModule.UserData.NEVER_COLLECTED);
            module.saveUserData(user);
        }

        @Subcommand("days reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetDayNum(DailyRewardsModule module, DailyRewardsModule.UserData user) {
            user.setDayNum(0);
            user.setLastCollectedDate(DailyRewardsModule.UserData.NEVER_COLLECTED);
            module.saveUserData(user);
        }

        @Subcommand("streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setStreak(DailyRewardsModule module, DailyRewardsModule.UserData user, int streak) {
            user.setStreak(streak);
            module.saveUserData(user);
        }

        @Subcommand("streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetStreak(DailyRewardsModule module, DailyRewardsModule.UserData user) {
            user.setStreak(0);
            module.saveUserData(user);
        }

        @Subcommand("highest-streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.set")
        // TODO: Add DailyRewardsModule ParameterType
        public void setHighestStreak(DailyRewardsModule module, DailyRewardsModule.UserData user, int highestStreak) {
            user.setHighestStreak(highestStreak);
            module.saveUserData(user);
        }

        @Subcommand("highest-streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.reset")
        // TODO: Add DailyRewardsModule ParameterType
        public void resetHighestStreak(DailyRewardsModule module, DailyRewardsModule.UserData user) {
            user.setHighestStreak(0);
            module.saveUserData(user);
        }
    }

    public static class PlaytimeRewards {

        @Subcommand("last-collected-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.lastcollectedplaytime.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setLastCollectedPlaytime(PlaytimeRewardsModule module, PlaytimeRewardsModule.UserData user, int lastCollectedPlaytime) {
            user.setLastCollectedPlaytime(lastCollectedPlaytime);
            module.saveUserData(user);
        }

        @Subcommand("start-date set")
        @CommandPermission("lushrewards.edituser.playtimerewards.startdate.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setStartDate(PlaytimeRewardsModule module, PlaytimeRewardsModule.UserData user, LocalDate startDate) {
            user.setStartDate(startDate);
            module.saveUserData(user);
        }

        @Subcommand("previous-day-end-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.previousdayendplaytime.set")
        // TODO: Add PlaytimeRewardsModule ParameterType
        public void setPreviousDayEndPlaytime(PlaytimeRewardsModule module, PlaytimeRewardsModule.UserData user, int previousDayEndPlaytime) {
            user.setPreviousDayEndPlaytime(previousDayEndPlaytime);
            module.saveUserData(user);
        }
    }
}
