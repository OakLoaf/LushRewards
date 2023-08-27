package me.dave.activityrewarder.module.playtimegoals;

import me.dave.activityrewarder.module.ModuleData;

import java.util.List;

public class PlaytimeGoalsModuleData extends ModuleData {
    private final int lastCollectedPlaytime;
    private final List<Integer> collectedTimes;

    public PlaytimeGoalsModuleData(String id, int lastCollectedPlaytime, List<Integer> collectedTimes) {
        super(id);
        this.lastCollectedPlaytime = lastCollectedPlaytime;
        this.collectedTimes = collectedTimes;
    }

    public int getLastCollectedPlaytime() {
        return lastCollectedPlaytime;
    }

    public List<Integer> getCollectedTimes() {
        return collectedTimes;
    }
}
