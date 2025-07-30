package org.lushplugins.lushrewards.user;

import org.bukkit.plugin.java.JavaPlugin;
import org.lushplugins.lushrewards.LushRewards;

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
}
