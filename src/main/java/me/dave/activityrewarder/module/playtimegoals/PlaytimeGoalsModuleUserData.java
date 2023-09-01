package me.dave.activityrewarder.module.playtimegoals;

import me.dave.activityrewarder.module.ModuleData;

import java.util.List;

public class PlaytimeGoalsModuleUserData extends ModuleData {
    private int lastCollectedPlaytime;
    private final List<Integer> collectedTimes;

    public PlaytimeGoalsModuleUserData(String id, int lastCollectedPlaytime, List<Integer> collectedTimes) {
        super(id);
        this.lastCollectedPlaytime = lastCollectedPlaytime;
        this.collectedTimes = collectedTimes;
    }

    public int getLastCollectedPlaytime() {
        return lastCollectedPlaytime;
    }

    public void setLastCollectedPlaytime(int lastCollectedPlaytime) {
        this.lastCollectedPlaytime = lastCollectedPlaytime;
    }

    public List<Integer> getCollectedTimes() {
        return collectedTimes;
    }

    public void addCollectedTime(int minutes) {
        collectedTimes.add(minutes);
    }
}
