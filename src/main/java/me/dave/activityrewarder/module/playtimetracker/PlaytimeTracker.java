package me.dave.activityrewarder.module.playtimetracker;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.utils.SimpleLocation;
import org.bukkit.entity.Player;

public class PlaytimeTracker {
    private static final int IDLE_TIME_TO_AFK = 300;
    private final Player player;
    private SimpleLocation lastLocation;
    private boolean afk;
    private int sessionTime;
    private int idleTime;
    private int globalTime;

    public PlaytimeTracker(Player player) {
        this.player = player;
        this.afk = false;
        this.sessionTime = 0;
        this.idleTime = 0;
        this.globalTime = ActivityRewarder.getDataManager().getRewardUser(player).getPlayMinutes();
        updateLocation();
    }

    public void tick() {
        if (!player.isOnline()) {
            PlaytimeTrackerModule playtimeTrackerModule = (PlaytimeTrackerModule) ActivityRewarder.getModule("playtime-tracker");
            if (playtimeTrackerModule != null) {
                playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId());
            }

            return;
        }

        if (hasMoved()) {
            updateLocation();
            whileActive();
        } else {
            whileInactive();
        }
    }

    public void whileActive() {
        sessionTime++;
        if (sessionTime % 60 == 0) {
            globalTime++;
        }

        if (afk) {
            idleTime = 0;
            afk = false;
        }
    }

    public void whileInactive() {
        idleTime++;

        if (!afk) {
            if (idleTime > IDLE_TIME_TO_AFK) {
                afk = true;
            } else {
                sessionTime++;
                if (sessionTime % 60 == 0) {
                    globalTime++;
                }
            }
        }
    }

    public void updateLocation() {
        this.lastLocation = SimpleLocation.from(player.getLocation());
    }

    public boolean hasMoved() {
        return !SimpleLocation.from(player.getLocation()).equals(lastLocation);
    }

    public int getIdlePlaytime() {
        return idleTime;
    }

    public int getSessionPlaytime() {
        return (int) Math.floor(sessionTime / 60f);
    }

    public int getTotalSessionPlaytime() {
        return sessionTime + idleTime;
    }

    public int getGlobalPlaytime() {
        return globalTime;
    }
}
