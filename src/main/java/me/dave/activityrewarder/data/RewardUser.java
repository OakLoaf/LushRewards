package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.ModuleData;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private int minutesPlayed;

    private final HashMap<String, ModuleData> moduleDataMap = new HashMap<>();

    public RewardUser(@NotNull UUID uuid, String username, int minutesPlayed) {
        this.uuid = uuid;
        this.username = username;
        this.minutesPlayed = minutesPlayed;
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

    @Nullable
    public ModuleData getModuleData(String module) {
        return moduleDataMap.get(module);
    }

    public void addModuleData(ModuleData moduleData) {
        moduleDataMap.put(moduleData.getId(), moduleData);
    }

    public int getDayNum() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getDayNum();
    }

    public void setDayNum(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).setDayNum(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    // TODO: Needs moving into ModuleData
    public int getStreakLength() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getStreakLength();
    }

    public void setStreakLength(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).setStreakLength(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void restartStreak() {
        DailyRewardsModuleUserData dailyRewardsModuleUserData = (DailyRewardsModuleUserData) getModuleData("daily-rewards");

        dailyRewardsModuleUserData.setDayNum(1);
        dailyRewardsModuleUserData.setStreakLength(1);
        dailyRewardsModuleUserData.clearCollectedDates();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementStreakLength() {
        ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).incrementStreakLength();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public int getHighestStreak() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getHighestStreak();
    }

    public LocalDate getLastCollectedDate() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getLastCollectedDate();
    }

    public void setLastCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).setLastCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public LocalDate getDateAtStreakLength(int dayNum) {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getDateAtStreakLength(dayNum);
    }

    public int getDayNumOffset() {
        return getDayNum() - ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getStreakLength();
    }

    public boolean hasCollectedToday() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).hasCollectedToday();
    }

    public List<String> getCollectedDates() {
        return ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).getCollectedDates();
    }

    public void addCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData("daily-rewards")).addCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }
}
