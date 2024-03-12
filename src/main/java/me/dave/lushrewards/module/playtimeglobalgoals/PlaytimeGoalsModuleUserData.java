package me.dave.lushrewards.module.playtimeglobalgoals;

import me.dave.lushrewards.module.UserDataModule;

public class PlaytimeGoalsModuleUserData extends UserDataModule.UserData {
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
