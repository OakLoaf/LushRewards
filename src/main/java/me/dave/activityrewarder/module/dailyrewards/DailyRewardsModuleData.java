package me.dave.activityrewarder.module.dailyrewards;

import me.dave.activityrewarder.module.ModuleData;
import me.dave.activityrewarder.utils.SimpleDate;

public class DailyRewardsModuleData extends ModuleData {
    private int dayNum;
    private int highestStreak;
    private final SimpleDate startDate;
    private SimpleDate lastCollectedDate;

    public DailyRewardsModuleData(String id, int dayNum, int highestStreak, SimpleDate startDate, SimpleDate lastCollectedDate) {
        super(id);
        this.dayNum = dayNum;
        this.highestStreak = highestStreak;
        this.startDate = startDate;
        this.lastCollectedDate = lastCollectedDate;
    }

    public int getDayNum() {
        return dayNum;
    }

    public void setDayNum(int dayNum) {
        this.dayNum = dayNum;
        this.lastCollectedDate = SimpleDate.now().minusDays(dayNum);
    }

    public void incrementDayNum() {
        this.dayNum += 1;
        if (dayNum > highestStreak) {
            highestStreak = dayNum;
        }
    }

    public SimpleDate getDateOnDayNum(int dayNum) {
        SimpleDate date = SimpleDate.now();
        date.addDays(dayNum - getActualDayNum());
        return date;
    }

    public int getHighestStreak() {
        return highestStreak;
    }

    public SimpleDate getStartDate() {
        return this.startDate;
    }

    public int getActualDayNum() {
        return (int) (SimpleDate.now().toEpochDay() - getStartDate().toEpochDay() + 1);
    }

    public SimpleDate getLastCollectedDate() {
        return this.lastCollectedDate;
    }

    public void setLastCollectedDate(SimpleDate date) {
        this.lastCollectedDate = date;
    }
}
