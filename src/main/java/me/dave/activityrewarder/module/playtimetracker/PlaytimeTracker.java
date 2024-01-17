package me.dave.activityrewarder.module.playtimetracker;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.utils.SimpleLocation;
import org.bukkit.entity.Player;

import java.util.Optional;

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
        this.globalTime = ActivityRewarder.getInstance().getDataManager().getRewardUser(player).getMinutesPlayed();
        updateLocation();
    }

    public void tick() {
        if (!player.isOnline()) {
            Optional<Module> optionalModule = ActivityRewarder.getInstance().getModule(PlaytimeTrackerModule.ID);
            if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId());
            }

            return;
        }

        if (ActivityRewarder.getInstance().getConfigManager().getPlaytimeIgnoreAfk()) {
            whileActive();
        } else {
            if (hasMoved()) {
                updateLocation();
                whileActive();
            } else {
                whileInactive();
            }
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

    public void saveData() {
        ActivityRewarder.getInstance().getDataManager().getRewardUser(player).setMinutesPlayed(globalTime);
    }

    private void incrementSessionTime() {
        sessionTime++;

        if (sessionTime % 60 == 0) {
            incrementGlobalTime();
        }
    }

    private void incrementGlobalTime() {
        globalTime++;

        if (player.hasPermission("activityrewarder.use")) {
            ActivityRewarder.getInstance().getModules().forEach(module -> {
                if (module instanceof PlaytimeDailyGoalsModule playtimeDailyGoalsModule) {
                    if (playtimeDailyGoalsModule.getRefreshTime() > 0 && globalTime % playtimeDailyGoalsModule.getRefreshTime() == 0) {
                        playtimeDailyGoalsModule.claimRewards(player);
                    }
                } else if (module instanceof PlaytimeGlobalGoalsModule playtimeGlobalGoalsModule) {
                    if (playtimeGlobalGoalsModule.getRefreshTime() > 0 && globalTime % playtimeGlobalGoalsModule.getRefreshTime() == 0) {
                        playtimeGlobalGoalsModule.claimRewards(player);
                    }
                }
            });
        }

        if (globalTime % 5 == 0) {
            ActivityRewarder.getInstance().getDataManager().getRewardUser(player).setMinutesPlayed(globalTime);
        }
    }

    public void updateLocation() {
        this.lastLocation = SimpleLocation.adapt(player.getLocation());
    }

    public boolean hasMoved() {
        return !SimpleLocation.adapt(player.getLocation()).equals(lastLocation);
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
