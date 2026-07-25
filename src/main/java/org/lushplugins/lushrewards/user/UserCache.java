package org.lushplugins.lushrewards.user;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.playtime.PlaytimeTracker;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserCache extends org.lushplugins.lushlib.cache.UserCache<RewardUser> {

    public UserCache(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected CompletableFuture<RewardUser> load(UUID uuid) {
        return LushRewards.getInstance().getStorageManager().loadRewardUser(uuid);
    }

    @Override
    public void onUserConnect(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.loadUser(player.getUniqueId(), true).thenAccept(user -> {
            user.setUsername(player.getName());
            LushRewards.getInstance().getPlaytimeTrackerManager().ifEnabled(playtimeTracker -> playtimeTracker.startPlaytimeTracker(player));
        });
    }

    @Override
    public void onUserDisconnect(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        RewardUser user = this.getCachedUser(uuid);
        if (user == null) {
            return;
        }

        LushRewards.getInstance().getPlaytimeTrackerManager().ifEnabled(playtimeTrackerManager -> {
            PlaytimeTracker playtimeTracker = playtimeTrackerManager.stopPlaytimeTracker(uuid);
            if (playtimeTracker != null) {
                user.setMinutesPlayed(playtimeTracker.getGlobalPlaytime());
            }
        });

        this.unloadUser(uuid);
        LushRewards.getInstance().getStorageManager().saveRewardUser(user);
    }
}
