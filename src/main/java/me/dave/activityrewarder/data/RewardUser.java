package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.ModuleData;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleData;
import me.dave.activityrewarder.utils.SimpleDate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    // TODO: Needs moving into ModuleData
    public int getDayNum() {
        return ((DailyRewardsModuleData) getModuleData("daily-rewards")).getDayNum();
    }

    public void setDay(int dayNum) {
        ((DailyRewardsModuleData) getModuleData("daily-rewards")).setDayNum(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void resetDays() {
        ((DailyRewardsModuleData) getModuleData("daily-rewards")).setDayNum(1);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementDayNum() {
        ((DailyRewardsModuleData) getModuleData("daily-rewards")).incrementDayNum();
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public SimpleDate getLastCollectedDate() {
        return ((DailyRewardsModuleData) getModuleData("daily-rewards")).getLastCollectedDate();
    }

    public void setLastDate(SimpleDate date) {
        ((DailyRewardsModuleData) getModuleData("daily-rewards")).setLastCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public SimpleDate getDateOnDayNum(int dayNum) {
        return ((DailyRewardsModuleData) getModuleData("daily-rewards")).getDateOnDayNum(dayNum);
    }

    public int getHighestStreak() {
        return ((DailyRewardsModuleData) getModuleData("daily-rewards")).getHighestStreak();
    }

    public int getActualDayNum() {
        return (int) (SimpleDate.now().toEpochDay() - ((DailyRewardsModuleData) getModuleData("daily-rewards")).getStartDate().toEpochDay() + 1);
    }

    public int getDayNumOffset() {
        return getActualDayNum() - ((DailyRewardsModuleData) getModuleData("daily-rewards")).getDayNum();
    }

    public boolean hasCollectedToday() {
        return SimpleDate.now().equals(((DailyRewardsModuleData) getModuleData("daily-rewards")).getLastCollectedDate());
    }

    public List<String> getCollectedDates() {
        return ((DailyRewardsModuleData) getModuleData("daily-rewards")).getCollectedDates();
    }

    public void addCollectedDate(SimpleDate date) {
        ((DailyRewardsModuleData) getModuleData("daily-rewards")).addCollectedDate(date);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }
}
