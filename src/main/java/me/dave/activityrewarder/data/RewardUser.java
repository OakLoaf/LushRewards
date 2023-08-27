package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleData;
import me.dave.activityrewarder.utils.SimpleDate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private int minutesPlayed;

    private final DailyRewardsModuleData dailyRewardsModuleData;
    private final PlaytimeGoalsModuleData dailyPlaytimeGoalsModuleData;
    private final PlaytimeGoalsModuleData globalPlaytimeGoalsModuleData;

    public RewardUser(@NotNull UUID uuid, String username, int minutesPlayed, @Nullable DailyRewardsModuleData dailyRewardsModuleData, @Nullable PlaytimeGoalsModuleData dailyPlaytimeGoalsModuleData, @Nullable PlaytimeGoalsModuleData globalPlaytimeGoalsModuleData) {
        this.uuid = uuid;
        this.username = username;
        this.minutesPlayed = minutesPlayed;

        this.dailyRewardsModuleData = dailyRewardsModuleData;
        this.dailyPlaytimeGoalsModuleData = dailyPlaytimeGoalsModuleData;
        this.globalPlaytimeGoalsModuleData = globalPlaytimeGoalsModuleData;
    }

    public UUID getUniqueId() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public int getMinutesPlayed() {
        return this.minutesPlayed;
    }

    public int getHoursPlayed() {
        return this.minutesPlayed * 60;
    }

    public void setMinutesPlayed(int minutesPlayed) {
        this.minutesPlayed = minutesPlayed;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public DailyRewardsModuleData getDailyRewardsModuleData() {
        return dailyRewardsModuleData;
    }

    public PlaytimeGoalsModuleData getDailyPlaytimeGoalsModuleData() {
        return dailyPlaytimeGoalsModuleData;
    }

    public PlaytimeGoalsModuleData getGlobalPlaytimeGoalsModuleData() {
        return globalPlaytimeGoalsModuleData;
    }

    // Outdated

    public int getDayNum() {
        return dailyRewardsModuleData.getDayNum();
    }

    public void setDay(int dayNum) {
        dailyRewardsModuleData.setDayNum(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void resetDays() {
        dailyRewardsModuleData.setDayNum(1);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementDayNum() {
        dailyRewardsModuleData.incrementDayNum();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public SimpleDate getStartDate() {
        return dailyRewardsModuleData.getStartDate();
    }

    public SimpleDate getLastCollectedDate() {
        return dailyRewardsModuleData.getLastCollectedDate();
    }

    public void setLastDate(SimpleDate date) {
        dailyRewardsModuleData.setLastCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }


    public SimpleDate getDateOnDayNum(int dayNum) {
        SimpleDate date = SimpleDate.now();
        date.addDays(dayNum - getActualDayNum());
        return date;
    }

    public int getHighestStreak() {
        return dailyRewardsModuleData.getHighestStreak();
    }

    public int getActualDayNum() {
        return (int) (SimpleDate.now().toEpochDay() - dailyRewardsModuleData.getStartDate().toEpochDay() + 1);
    }

    public int getDayNumOffset() {
        return getActualDayNum() - dailyRewardsModuleData.getDayNum();
    }

    public boolean hasCollectedToday() {
        return SimpleDate.now().equals(dailyRewardsModuleData.getLastCollectedDate());
    }

    public record PlaytimeGoalsModuleData(int lastCollectedPlaytime, List<Integer> collectedTimes) {}
}
