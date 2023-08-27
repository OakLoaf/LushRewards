package me.dave.activityrewarder.module.playtimetracker;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.playtimegoals.PlaytimeGoalsModuleData;
import me.dave.activityrewarder.module.playtimegoals.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimegoals.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
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
        this.globalTime = ActivityRewarder.getDataManager().getRewardUser(player).getMinutesPlayed();
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
        incrementSessionTime();

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
                incrementSessionTime();
            }
        }
    }

    private void incrementSessionTime() {
        sessionTime++;

        if (sessionTime % 60 == 0) {
            incrementGlobalTime();
        }
    }

    private void incrementGlobalTime() {
        globalTime++;

        if (ActivityRewarder.getModule("playtime-daily-goals") instanceof PlaytimeDailyGoalsModule playtimeDailyGoalsModule) {
            if (playtimeDailyGoalsModule.getRefreshTime() > 0 && globalTime % playtimeDailyGoalsModule.getRefreshTime() == 0) {
                playtimeDailyGoalsModule.getRewardCollectionsInRange(((PlaytimeGoalsModuleData) ActivityRewarder.getDataManager().getRewardUser(player).getModuleData("playtime-daily-goals")).getLastCollectedPlaytime(), globalTime).forEach(rewardCollection -> rewardCollection.giveAll(player));
            }
        }

        if (ActivityRewarder.getModule("playtime-global-goals") instanceof PlaytimeGlobalGoalsModule playtimeGlobalGoalsModule) {
            if (playtimeGlobalGoalsModule.getRefreshTime() > 0 && globalTime % playtimeGlobalGoalsModule.getRefreshTime() == 0) {
                playtimeGlobalGoalsModule.getRewardCollectionsInRange(((PlaytimeGoalsModuleData) ActivityRewarder.getDataManager().getRewardUser(player).getModuleData("playtime-daily-goals")).getLastCollectedPlaytime(), globalTime).forEach(rewardCollection -> rewardCollection.giveAll(player));
            }
        }

        if (globalTime % 15 == 0) {
            ActivityRewarder.getDataManager().getRewardUser(player).setMinutesPlayed(globalTime);
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
        return (int) Math.floor((sessionTime + idleTime) / 60f);
    }

    public int getGlobalPlaytime() {
        return globalTime;
    }
}
