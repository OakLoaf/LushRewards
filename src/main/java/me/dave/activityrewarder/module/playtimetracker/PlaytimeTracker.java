package me.dave.activityrewarder.module.playtimetracker;

import me.dave.activityrewarder.utils.SimpleLocation;
import org.bukkit.entity.Player;

public class PlaytimeTracker {
    private static final int IDLE_TIME_TO_AFK = 300;
    private final Player player;
    private SimpleLocation lastLocation;
    private boolean afk;
    private int sessionTime;
    private int idleTime;

    public PlaytimeTracker(Player player) {
        this.player = player;
        this.afk = false;
        this.sessionTime = 0;
        this.idleTime = 0;
        updateLocation();
    }

    public void tick() {
        if (!player.isOnline()) {
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

        if (afk) {
            idleTime = 0;
            afk = false;
        }
    }

    public void whileInactive() {
        idleTime++;

        if (!afk && idleTime > IDLE_TIME_TO_AFK) {
            afk = true;
        } else {
            sessionTime++;
        }
    }

    public void updateLocation() {
        this.lastLocation = SimpleLocation.from(player.getLocation());
    }

    public boolean hasMoved() {
        return !SimpleLocation.from(player.getLocation()).equals(lastLocation);
    }

    public int getSessionTime() {
        return sessionTime;
    }
}