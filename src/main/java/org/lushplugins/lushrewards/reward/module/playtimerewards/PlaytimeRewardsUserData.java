package org.lushplugins.lushrewards.reward.module.playtimerewards;

import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonCreator;
import org.lushplugins.lushlib.libraries.jackson.annotation.JsonProperty;
import org.lushplugins.lushrewards.user.ModuleUserData;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

public class PlaytimeRewardsUserData extends ModuleUserData {
    private int lastCollectedPlaytime;
    private LocalDate startDate;
    private int previousDayEndPlaytime;

    public PlaytimeRewardsUserData(
        UUID uuid,
        String moduleId,
        int lastCollectedPlaytime,
        @NotNull LocalDate startDate,
        int previousDayEndPlaytime
    ) {
        super(uuid, moduleId);
        this.lastCollectedPlaytime = lastCollectedPlaytime;
        this.startDate = startDate;
        this.previousDayEndPlaytime = previousDayEndPlaytime;
    }

    public PlaytimeRewardsUserData(UUID uuid, String moduleId) {
        this(uuid, moduleId, 0, LocalDate.now(), 0);
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
