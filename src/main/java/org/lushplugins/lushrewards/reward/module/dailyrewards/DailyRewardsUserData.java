package org.lushplugins.lushrewards.reward.module.dailyrewards;

import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonCreator;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonProperty;
import org.lushplugins.lushrewards.user.ModuleUserData;

import java.time.LocalDate;
import java.util.HashSet;

public class DailyRewardsUserData extends ModuleUserData {
    public static final LocalDate NEVER_COLLECTED = LocalDate.of(1971, 10, 1); // The date Walt Disney World was opened

    private LocalDate startDate;
    private LocalDate lastJoinDate;
    private LocalDate lastCollectedDate;
    private final HashSet<Integer> collectedDays;
    private int dayNum;
    private int streak;
    private int highestStreak;

    @JsonCreator
    public DailyRewardsUserData(
        @JsonProperty LocalDate lastJoinDate,
        @JsonProperty int dayNum,
        @JsonProperty int streak,
        @JsonProperty int highestStreak,
        @JsonProperty LocalDate startDate,
        @JsonProperty LocalDate lastCollectedDate,
        @JsonProperty HashSet<Integer> collectedDays
    ) {
        this.startDate = startDate;
        this.lastJoinDate = lastJoinDate;
        this.lastCollectedDate = lastCollectedDate;
        this.collectedDays = collectedDays;
        this.dayNum = dayNum;
        this.streak = streak;
        this.highestStreak = highestStreak;
    }

    public DailyRewardsUserData() {
        this(null, 1, 0, 0, LocalDate.now(), null, new HashSet<>());
    }

    public LocalDate getLastJoinDate() {
        return lastJoinDate;
    }

    public void setLastJoinDate(LocalDate lastJoinDate) {
        this.lastJoinDate = lastJoinDate;
    }

    public int getDayNum() {
        return dayNum;
    }

    public void setDayNum(int dayNum) {
        this.dayNum = dayNum;
    }

    public void incrementDayNum() {
        this.dayNum++;
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = streak;
        if (streak > highestStreak) {
            highestStreak = streak;
        }
    }

    public void incrementStreak() {
        setStreak(this.streak + 1);
    }

    public int getHighestStreak() {
        return highestStreak;
    }

    public void setHighestStreak(int highestStreak) {
        this.highestStreak = highestStreak;
    }

    public LocalDate getExpectedDateOnDayNum(int dayNum) {
        return LocalDate.now().plusDays(dayNum - getDayNum());
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public void setStartDate(LocalDate date) {
        this.startDate = date;
    }

    @Nullable
    public LocalDate getLastCollectedDate() {
        return this.lastCollectedDate;
    }

    public void setLastCollectedDate(LocalDate date) {
        this.lastCollectedDate = date;
    }

    public boolean hasCollectedToday() {
        return lastCollectedDate != null && lastCollectedDate.isEqual(LocalDate.now());
    }

    public HashSet<Integer> getCollectedDays() {
        return collectedDays;
    }

    public boolean hasCollectedDay(int dayNum) {
        return collectedDays.contains(dayNum);
    }

    public void addCollectedDay(int dayNum) {
        collectedDays.add(dayNum);
    }

    public void clearCollectedDays() {
        collectedDays.clear();
    }
}
