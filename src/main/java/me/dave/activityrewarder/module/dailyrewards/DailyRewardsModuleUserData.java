package me.dave.activityrewarder.module.dailyrewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.ModuleData;
import me.dave.activityrewarder.utils.SimpleDate;

import java.util.List;

public class DailyRewardsModuleUserData extends ModuleData {
    private int streakLength;
    private int highestStreak;
    private SimpleDate startDate;
    private SimpleDate lastCollectedDate;
    private final List<String> collectedDates;

    public DailyRewardsModuleUserData(String id, int streakLength, int highestStreak, SimpleDate startDate, SimpleDate lastCollectedDate, List<String> collectedDates) {
        super(id);
        this.streakLength = streakLength;
        this.highestStreak = highestStreak;
        this.startDate = startDate;
        this.lastCollectedDate = lastCollectedDate;
        this.collectedDates = collectedDates;
    }

    public int getDayNum() {
        int dayNum = (int) (SimpleDate.now().toEpochDay() - startDate.toEpochDay());

        if (ActivityRewarder.getModule("daily-rewards") instanceof DailyRewardsModule dailyRewardsModule) {
            int resetDay = dailyRewardsModule.getResetDay();

            if (resetDay > 0 && dayNum > resetDay) {
                setDayNum(1);
                dayNum = 1;
            }
        }

        return dayNum;
    }

    public void setDayNum(int dayNum) {
        startDate = SimpleDate.now().minusDays(dayNum);
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

    public SimpleDate getDateAtStreakLength(int streakLength) {
        SimpleDate date = SimpleDate.now();
        date.addDays(streakLength - getDayNum());
        return date;
    }

    public SimpleDate getStartDate() {
        return this.startDate;
    }

    public SimpleDate getLastCollectedDate() {
        return this.lastCollectedDate;
    }

    public void setLastCollectedDate(SimpleDate date) {
        this.lastCollectedDate = date;
    }

    public List<String> getCollectedDates() {
        return collectedDates;
    }

    public boolean hasCollectedToday() {
        return collectedDates.contains(SimpleDate.now().toString("dd-mm-yyyy"));
    }

    public void addCollectedDate(SimpleDate date) {
        collectedDates.add(date.toString("dd-mm-yyyy"));
    }

    public void clearCollectedDates() {
        collectedDates.clear();
    }
}
