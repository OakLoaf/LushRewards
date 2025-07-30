package org.lushplugins.lushrewards.playtimetracker;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushlib.utils.SimpleLocation;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlaytimeTracker {
    private static final int IDLE_TIME_TO_AFK = 300;
    private final Player player;
    private SimpleLocation lastLocation;
    private boolean afk;
    /**
     * Current session time (in seconds)
     */
    private int sessionTime;
    /**
     * Current idle time (in seconds), returns {@code 0} if not idle
     */
    private int idleTime;
    /**
     * All-time playtime (in minutes), this value excludes idle time
     */
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

        if (LushRewards.getInstance().getConfigManager().getPlaytimeIgnoreAfk()) {
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

    // TODO: Remove?
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
            for (PlaytimeRewardsModule module : LushRewards.getInstance().getRewardModuleManager().getModules(PlaytimeRewardsModule.class)) {
                if (!player.hasPermission("lushrewards.use." + module.getId())) {
                    continue;
                }

                if (module.getRefreshTime() > 0 && globalTime % module.getRefreshTime() == 0) {
                    module.claimRewards(player);
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
}
