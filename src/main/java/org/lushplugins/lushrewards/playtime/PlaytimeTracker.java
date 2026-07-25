package org.lushplugins.lushrewards.playtime;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushlib.utils.SimpleLocation;
import org.bukkit.entity.Player;

public class PlaytimeTracker {
    /**
     * Time idle before marked as inactive (in seconds)
     */
    private static final int IDLE_TIME_TO_AFK = 300;

    private final Player player;
    private SimpleLocation lastLocation;
    private boolean afk = false;
    /**
     * All-time playtime (in minutes), this value excludes idle time
     */
    private int globalTime;
    /**
     * Current session time (in seconds)
     */
    private int sessionTime = 0;
    /**
     * Current idle time (in seconds), returns {@code 0} if not idle
     */
    private int idleTime = 0;

    public PlaytimeTracker(Player player) {
        RewardUser rewardUser = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
        if (rewardUser == null) {
            throw new IllegalStateException("Failed to find reward user for user: " + player.getName() + "(" + player.getUniqueId() + ")");
        }

        this.player = player;
        this.globalTime = rewardUser.getMinutesPlayed();
        this.lastLocation = SimpleLocation.adapt(player.getLocation());
    }

    public Player getPlayer() {
        return player;
    }

    public boolean hasMoved() {
        return !SimpleLocation.adapt(player.getLocation()).equals(lastLocation);
    }

    public int getSessionPlaytime() {
        return (int) Math.floor(sessionTime / 60f);
    }

    public int getTotalSessionPlaytime() {
        return (int) Math.floor((sessionTime + idleTime) / 60f);
    }

    public boolean isIdle() {
        return idleTime == 0;
    }

    public int getIdlePlaytime() {
        return idleTime;
    }

    public int getGlobalPlaytime() {
        return globalTime;
    }

    public void setGlobalPlaytime(int globalPlaytime) {
        globalTime = globalPlaytime;
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

    public boolean tick() {
        if (!player.isOnline()) {
            return false;
        }

        if (LushRewards.getInstance().getConfigManager().shouldPlaytimeTrackerIgnoreAfk()) {
            whileActive();
        } else {
            if (hasMoved()) {
                this.lastLocation = SimpleLocation.adapt(player.getLocation());
                whileActive();
            } else {
                whileInactive();
            }
        }

        return true;
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
            RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
            for (PlaytimeRewardsModule module : LushRewards.getInstance().getRewardModuleManager().getModules(PlaytimeRewardsModule.class)) {
                if (!player.hasPermission("lushrewards.use." + module.getId())) {
                    continue;
                }

                if (module.getRefreshTime() > 0 && globalTime % module.getRefreshTime() == 0) {
                    module.claimRewards(player, user);
                }
            }
        }

        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
        if (user != null) {
            user.setMinutesPlayed(globalTime, globalTime % 5 == 0);
        } else {
            LushRewards.getInstance().getPlaytimeTrackerManager().ifEnabled(playtimeTrackerManager -> {
                playtimeTrackerManager.stopPlaytimeTracker(player.getUniqueId());
            });
        }
    }
}
