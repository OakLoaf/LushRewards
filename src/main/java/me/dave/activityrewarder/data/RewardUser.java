package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.Module;
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

    // TODO: Add reload modules

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
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getDayNum();
    }

    public void setDayNum(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).setDayNum(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    // TODO: Needs moving into ModuleData
    public int getStreakLength() {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getStreakLength();
    }

    public void setStreakLength(int dayNum) {
        ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).setStreakLength(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void restartStreak() {
        DailyRewardsModuleUserData dailyRewardsModuleUserData = (DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName());

        dailyRewardsModuleUserData.setDayNum(1);
        dailyRewardsModuleUserData.setStreakLength(1);
        dailyRewardsModuleUserData.clearCollectedDates();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementStreakLength() {
        ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).incrementStreakLength();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public int getHighestStreak() {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getHighestStreak();
    }

    @Nullable
    public LocalDate getLastCollectedDate() {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getLastCollectedDate();
    }

    public void setLastCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).setLastCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public LocalDate getDateAtStreakLength(int dayNum) {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getDateAtStreakLength(dayNum);
    }

    public int getDayNumOffset() {
        return getDayNum() - ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getStreakLength();
    }

    public boolean hasCollectedToday() {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).hasCollectedToday();
    }

    public List<String> getCollectedDates() {
        return ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).getCollectedDates();
    }

    public void addCollectedDate(LocalDate date) {
        ((DailyRewardsModuleUserData) getModuleData(Module.ModuleType.DAILY_REWARDS.getName())).addCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }
}
