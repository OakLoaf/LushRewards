package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;

import java.time.LocalDate;
import java.util.UUID;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private LocalDate startDate;
    private LocalDate lastDate;
    private int dayNum;
    private int highestStreak;
    private int playMinutes;

    public RewardUser(UUID uuid, String username, String startDate, String lastCollectedDate, int dayNum, int highestStreak, int playMinutes) {
        this.uuid = uuid;
        this.username = username;
        this.startDate = LocalDate.parse(startDate);
        this.lastDate = LocalDate.parse(lastCollectedDate);
        this.dayNum = dayNum;
        this.highestStreak = highestStreak;
        this.playMinutes = playMinutes;
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
        if (dayNum > highestStreak) {
            highestStreak = dayNum;
        }

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

    public void setPlayMinutes(int playMinutes) {
        this.playMinutes = playMinutes;
        ActivityRewarder.getDataManager().saveRewardUser(this);
    }

    public void increasePlayMinutes(int playMinutes) {
        this.playMinutes += playMinutes;
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

    public int getPlayMinutes() {
        return this.playMinutes;
    }

    public int getPlayHours() {
        return this.playMinutes * 60;
    }

    public int getTotalPlayTime() {
        return getTicksToHours(Bukkit.getPlayer(uuid).getStatistic(Statistic.PLAY_ONE_MINUTE));
    }

    public int getPlayTimeSinceLastCollected() {
        // Gets the player's current play time
        int currPlayTime = getTotalPlayTime();
        // Finds the difference between their current play time and play time when the player last collected their reward
        return currPlayTime - playMinutes;
    }

    public boolean hasCollectedToday() {
        return LocalDate.now().equals(lastDate);
    }

    private int getTicksToSeconds(long ticks) {
        return (int) Math.floor(ticks * 20);
    }

    private int getTicksToMinutes(long ticks) {
        return getTicksToSeconds(ticks) * 60;
    }

    private int getTicksToHours(long ticks) {
        return getTicksToMinutes(ticks) * 60;
    }
}
