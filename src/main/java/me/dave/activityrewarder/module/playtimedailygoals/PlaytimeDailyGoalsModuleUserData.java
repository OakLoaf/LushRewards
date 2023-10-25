package me.dave.activityrewarder.module.playtimedailygoals;

import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public class PlaytimeDailyGoalsModuleUserData extends PlaytimeGoalsModuleUserData {
    private LocalDate date;
    private int previousDayEndPlaytime;

    public PlaytimeDailyGoalsModuleUserData(String id, int lastCollectedPlaytime, @NotNull LocalDate date, int previousDayEndPlaytime) {
        super(id, lastCollectedPlaytime);
        this.date = date;
        this.previousDayEndPlaytime = previousDayEndPlaytime;
    }

    @NotNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NotNull LocalDate date) {
        this.date = date;
    }

    public int getPreviousDayEndPlaytime() {
        return previousDayEndPlaytime;
    }

    public void setPreviousDayEndPlaytime(int previousDayEndPlaytime) {
        this.previousDayEndPlaytime = previousDayEndPlaytime;
    }
}