package me.dave.activityrewarder.module.playtimedailygoals;

import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;

import java.time.LocalDate;

public class PlaytimeDailyGoalsModuleUserData extends PlaytimeGoalsModuleUserData {
    private LocalDate date;

    public PlaytimeDailyGoalsModuleUserData(String id, int lastCollectedPlaytime, LocalDate date) {
        super(id, lastCollectedPlaytime);
        this.date = date;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}