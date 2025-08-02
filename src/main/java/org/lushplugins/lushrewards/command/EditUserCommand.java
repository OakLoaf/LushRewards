package org.lushplugins.lushrewards.command;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsUserData;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.time.LocalDate;

//@SuppressWarnings("unused")
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

        @Subcommand("<user> days set")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.set")
        // TODO: Add DailyRewardsUserData ParameterType
        public void setDayNum(DailyRewardsUserData user, int dayNum) {
            user.setDayNum(dayNum);
            user.setLastCollectedDate(DailyRewardsUserData.NEVER_COLLECTED);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> days reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.daynum.reset")
        // TODO: Add DailyRewardsUserData ParameterType
        public void resetDayNum(DailyRewardsUserData user) {
            user.setDayNum(0);
            user.setLastCollectedDate(DailyRewardsUserData.NEVER_COLLECTED);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.set")
        // TODO: Add DailyRewardsUserData ParameterType
        public void setStreak(DailyRewardsUserData user, int streak) {
            user.setStreak(streak);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.streak.reset")
        // TODO: Add DailyRewardsUserData ParameterType
        public void resetStreak(DailyRewardsUserData user) {
            user.setStreak(0);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> highest-streak set")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.set")
        // TODO: Add DailyRewardsUserData ParameterType
        public void setHighestStreak(DailyRewardsUserData user, int highestStreak) {
            user.setHighestStreak(highestStreak);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> highest-streak reset")
        @CommandPermission("lushrewards.edituser.dailyrewards.higheststreak.reset")
        // TODO: Add DailyRewardsUserData ParameterType
        public void resetHighestStreak(DailyRewardsUserData user) {
            user.setHighestStreak(0);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }
    }

    // TODO: Migrate to Orphan command
    public static class PlaytimeRewards {

        @Subcommand("<user> last-collected-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.lastcollectedplaytime.set")
        // TODO: Add PlaytimeRewardsUserData ParameterType
        public void setLastCollectedPlaytime(PlaytimeRewardsUserData user, int lastCollectedPlaytime) {
            user.setLastCollectedPlaytime(lastCollectedPlaytime);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> start-date set")
        @CommandPermission("lushrewards.edituser.playtimerewards.startdate.set")
        // TODO: Add PlaytimeRewardsUserData ParameterType
        public void setStartDate(PlaytimeRewardsUserData user, LocalDate startDate) {
            user.setStartDate(startDate);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }

        @Subcommand("<user> previous-day-end-playtime set")
        @CommandPermission("lushrewards.edituser.playtimerewards.previousdayendplaytime.set")
        // TODO: Add PlaytimeRewardsUserData ParameterType
        public void setPreviousDayEndPlaytime(PlaytimeRewardsUserData user, int previousDayEndPlaytime) {
            user.setPreviousDayEndPlaytime(previousDayEndPlaytime);

            LushRewards.getInstance().getStorageManager().saveModuleUserData(user);
        }
    }
}
