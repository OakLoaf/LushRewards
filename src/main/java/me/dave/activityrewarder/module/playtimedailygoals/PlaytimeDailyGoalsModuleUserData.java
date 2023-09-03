package me.dave.activityrewarder.module.playtimedailygoals;

import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;

public class PlaytimeDailyGoalsModuleUserData extends PlaytimeGoalsModuleUserData {
    private LocalDate date;

    public PlaytimeDailyGoalsModuleUserData(String id, int lastCollectedPlaytime, @NotNull LocalDate date) {
        super(id, lastCollectedPlaytime);
        this.date = date;
    }

    @NotNull
    public LocalDate getDate() {
        return date;
    }

    public void setDate(@NotNull LocalDate date) {
        this.date = date;
    }
}