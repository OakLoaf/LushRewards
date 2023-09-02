package me.dave.activityrewarder.module.dailyrewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.module.ModuleData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DailyRewardsModuleUserData extends ModuleData {
    private int streakLength;
    private int highestStreak;
    private LocalDate startDate;
    private LocalDate lastCollectedDate;
    private final List<String> collectedDates;

    public DailyRewardsModuleUserData(String id, int streakLength, int highestStreak, LocalDate startDate, LocalDate lastCollectedDate, List<String> collectedDates) {
        super(id);
        this.streakLength = streakLength;
        this.highestStreak = highestStreak;
        this.startDate = startDate;
        this.lastCollectedDate = lastCollectedDate;
        this.collectedDates = collectedDates;
    }

    public int getDayNum() {
        int dayNum = (int) (LocalDate.now().toEpochDay() - startDate.toEpochDay());

        if (ActivityRewarder.getModule(Module.ModuleType.DAILY_REWARDS.getName()) instanceof DailyRewardsModule dailyRewardsModule) {
            int resetDay = dailyRewardsModule.getResetDay();

            if (resetDay > 0 && dayNum > resetDay) {
                setDayNum(1);
                dayNum = 1;
            }
        }

        return dayNum;
    }

    public void setDayNum(int dayNum) {
        startDate = LocalDate.now().minusDays(dayNum);
    }

    public int getStreakLength() {
        return streakLength;
    }

    public void setStreakLength(int streakLength) {
        this.streakLength = streakLength;
    }

    public void incrementStreakLength() {
        this.streakLength += 1;
        if (streakLength > highestStreak) {
            highestStreak = streakLength;
        }
    }

    public int getHighestStreak() {
        return highestStreak;
    }

    public LocalDate getDateAtStreakLength(int streakLength) {
        return LocalDate.now().plusDays(streakLength - getDayNum());
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getLastCollectedDate() {
        return this.lastCollectedDate;
    }

    public void setLastCollectedDate(LocalDate date) {
        this.lastCollectedDate = date;
    }

    public List<String> getCollectedDates() {
        return collectedDates;
    }

    public boolean hasCollectedToday() {
        return collectedDates.contains(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    public void addCollectedDate(LocalDate date) {
        collectedDates.add(date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    public void clearCollectedDates() {
        collectedDates.clear();
    }
}
