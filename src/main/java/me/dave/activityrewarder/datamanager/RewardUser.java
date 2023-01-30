package me.dave.activityrewarder.datamanager;

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
    private int playTime;
    private double hourlyMultiplier;

    public RewardUser(UUID uuid, String username, String startDate, String lastCollectedDate, int dayNum, int playTime) {
        this.uuid = uuid;
        this.username = username;
        this.startDate = LocalDate.parse(startDate);
        this.lastDate = LocalDate.parse(lastCollectedDate);
        this.dayNum = dayNum;
        this.playTime = playTime;

        this.hourlyMultiplier = ActivityRewarder.configManager.getHourlyMultiplier(Bukkit.getPlayer(uuid));
    }

    public void setUsername(String username) {
        this.username = username;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void setLastDate(String lastCollectedDate) {
        this.lastDate = LocalDate.parse(lastCollectedDate);
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void incrementDayNum() {
        this.dayNum += 1;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void resetDays() {
        this.dayNum = 1;
        this.startDate = LocalDate.now();
        this.lastDate = LocalDate.now().minusDays(1);
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void setHourlyMultiplier(double multiplier) {
        this.hourlyMultiplier = multiplier;
        ActivityRewarder.dataManager.saveRewardUser(this);
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
