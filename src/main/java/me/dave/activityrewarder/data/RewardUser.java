package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private LocalDate startDate;
    private LocalDate lastDate;
    private int dayNum;
    private int highestStreak;
    private int playTime;
    private double hourlyMultiplier;

    public RewardUser(UUID uuid, String username, String startDate, String lastCollectedDate, int dayNum, int highestStreak, int playTime) {
        this.uuid = uuid;
        this.username = username;
        this.startDate = LocalDate.parse(startDate);
        this.lastDate = LocalDate.parse(lastCollectedDate);
        this.dayNum = dayNum;
        this.highestStreak = highestStreak;
        this.playTime = playTime;

        this.hourlyMultiplier = ActivityRewarder.getConfigManager().getHourlyMultiplier(Bukkit.getPlayer(uuid));
    }

    public void setUsername(String username) {
        this.username = username;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void setLastDate(String lastCollectedDate) {
        this.lastDate = LocalDate.parse(lastCollectedDate);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void incrementDayNum() {
        this.dayNum += 1;
        if (dayNum > highestStreak) highestStreak = dayNum;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void setDay(int dayNum) {
        this.dayNum = dayNum;
        this.startDate = LocalDate.now();
        this.lastDate = LocalDate.now().minusDays(dayNum);
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void resetDays() {
        setDay(1);
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void setHourlyMultiplier(double multiplier) {
        this.hourlyMultiplier = multiplier;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getLastDate() {
        return this.lastDate;
    }

    public int getDayNum() {
        return this.dayNum;
    }

    public int getHighestStreak() {
        return this.highestStreak;
    }

    public int getActualDayNum() {
        return (int) (LocalDate.now().toEpochDay() - startDate.toEpochDay() + 1);
    }

    public int getDayNumOffset() {
        return getActualDayNum() - dayNum;
    }

    public int getPlayTime() {
        return this.playTime;
    }

    public int getTotalPlayTime() {
        return getTicksToHours(Bukkit.getPlayer(uuid).getStatistic(Statistic.PLAY_ONE_MINUTE));
    }

    public int getPlayTimeSinceLastCollected() {
        // Gets the player's current play time
        int currPlayTime = getTotalPlayTime();
        // Finds the difference between their current play time and play time when the player last collected their reward
        return currPlayTime - playTime;
    }

    public double getHourlyMultiplier() {
        return hourlyMultiplier;
    }

    public boolean hasCollectedToday() {
        return LocalDate.now().equals(lastDate);
    }

    private int getTicksToHours(long ticksPlayed) {
        return (int) TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
