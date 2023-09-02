package me.dave.activityrewarder.module.playtimeglobalgoals;

import me.dave.activityrewarder.module.ModuleData;

public class PlaytimeGoalsModuleUserData extends ModuleData {
    private int lastCollectedPlaytime;

    public PlaytimeGoalsModuleUserData(String id, int lastCollectedPlaytime) {
        super(id);
        this.lastCollectedPlaytime = lastCollectedPlaytime;
    }

    public int getLastCollectedPlaytime() {
        return lastCollectedPlaytime;
    }

    public void setLastCollectedPlaytime(int lastCollectedPlaytime) {
        this.lastCollectedPlaytime = lastCollectedPlaytime;
    }
}
