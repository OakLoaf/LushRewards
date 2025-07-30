package org.lushplugins.lushrewards.reward.module.playtimerewards;

import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonCreator;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonProperty;
import org.lushplugins.lushrewards.user.ModuleUserData;

import java.time.LocalDate;

public class PlaytimeRewardsUserData extends ModuleUserData {
    private int lastCollectedPlaytime;
    private LocalDate startDate;
    private int previousDayEndPlaytime;

    @JsonCreator
    public PlaytimeRewardsUserData(
        @JsonProperty int lastCollectedPlaytime,
        @JsonProperty @NotNull LocalDate startDate,
        @JsonProperty int previousDayEndPlaytime
    ) {
        this.lastCollectedPlaytime = lastCollectedPlaytime;
        this.startDate = startDate;
        this.previousDayEndPlaytime = previousDayEndPlaytime;
    }

    public int getLastCollectedPlaytime() {
        return lastCollectedPlaytime;
    }

    public void setLastCollectedPlaytime(int lastCollectedPlaytime) {
        this.lastCollectedPlaytime = lastCollectedPlaytime;
    }

    public @NotNull LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(@NotNull LocalDate startDate) {
        this.startDate = startDate;
    }

    public int getPreviousDayEndPlaytime() {
        return previousDayEndPlaytime;
    }

    public void setPreviousDayEndPlaytime(int previousDayEndPlaytime) {
        this.previousDayEndPlaytime = previousDayEndPlaytime;
    }
}
