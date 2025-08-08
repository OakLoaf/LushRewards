package org.lushplugins.lushrewards.module.playtimetracker;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushlib.utils.SimpleLocation;
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
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        if (rewardUser == null) {
            throw new IllegalStateException("Failed to find reward user for user: " + player.getName() + "(" + player.getUniqueId() + ")");
        }

        this.player = player;
        this.afk = false;
        this.sessionTime = 0;
        this.idleTime = 0;
        this.globalTime = rewardUser.getMinutesPlayed();
        updateLocation();
    }

    public void tick() {
        if (!player.isOnline()) {
            Optional<Module> optionalModule = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER);
            if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId());
            }

            return;
        }

        if (!LushRewards.getInstance().getConfigManager().shouldPlaytimeTrackerIgnoreAfk()) {
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
        LushRewards.getInstance().getDataManager().getOrLoadRewardUser(player.getUniqueId(), false).thenAccept(rewardUser -> rewardUser.setMinutesPlayed(globalTime));
    }

    private void incrementSessionTime() {
        sessionTime++;

        if (sessionTime % 60 == 0) {
            incrementGlobalTime();
        }
    }

    private void incrementGlobalTime() {
        globalTime++;

        if (player.hasPermission("lushrewards.use")) {
            for (Module module : LushRewards.getInstance().getModules()) {
                if (!player.hasPermission("lushrewards.use." + module.getId())) {
                    continue;
                }

                if (module instanceof PlaytimeRewardsModule playtimeRewardsModule) {
                    if (playtimeRewardsModule.getRefreshTime() > 0 && globalTime % playtimeRewardsModule.getRefreshTime() == 0) {
                        playtimeRewardsModule.claimRewards(player);
                    }
                }
            }
        }

        if (globalTime % 5 == 0) {
            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser != null) {
                rewardUser.setMinutesPlayed(globalTime);
            } else {
                Optional<Module> optionalModule = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER);
                if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                    playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId());
                }
            }
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

    public void setGlobalPlaytime(int globalPlaytime) {
        globalTime = globalPlaytime;
    }
}
