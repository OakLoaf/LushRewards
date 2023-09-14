package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.ModuleData;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private int minutesPlayed;

    private final ConcurrentHashMap<String, ModuleData> moduleDataMap = new ConcurrentHashMap<>();

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
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getDayNum();
    }

    public void setDayNum(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).setDayNum(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    // TODO: Needs moving into ModuleData
    public int getStreakLength() {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getStreakLength();
    }

    public void setStreakLength(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).setStreakLength(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void restartStreak() {
        DailyRewardsModuleUserData dailyRewardsModuleUserData = (DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID);

        dailyRewardsModuleUserData.setDayNum(1);
        dailyRewardsModuleUserData.setStreakLength(1);
        dailyRewardsModuleUserData.clearCollectedDates();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementStreakLength() {
        ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).incrementStreakLength();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public int getHighestStreak() {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getHighestStreak();
    }

    @Nullable
    public LocalDate getLastCollectedDate() {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getLastCollectedDate();
    }

    public void setLastCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).setLastCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public LocalDate getDateAtStreakLength(int dayNum) {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getDateAtStreakLength(dayNum);
    }

    public int getDayNumOffset() {
        return getDayNum() - ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getStreakLength();
    }

    public boolean hasCollectedToday() {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).hasCollectedToday();
    }

    public List<String> getCollectedDates() {
        return ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).getCollectedDates();
    }

    public void addCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData(DailyRewardsModule.ID)).addCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }
}
