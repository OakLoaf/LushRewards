package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.enchantedskies.activityrewarder.ActivityRewarder;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DataManager {
    private Storage storage;
    private final HashMap<UUID, RewardUser> uuidToRewardUser = new HashMap<>();

    // Safe to use bukkit api in callback.
    public void initAsync(Consumer<Boolean> onComplete) {
        Storage.SERVICE.submit(() -> {
            storage = new YmlStorage();

            final boolean init = storage.init();
            new BukkitRunnable() {
                @Override
                public void run() {
                    onComplete.accept(init);
                }
            }.runTask(ActivityRewarder.getInstance());
        });
    }

    public void loadRewardUser(UUID uuid, Runnable onComplete) {
        Storage.SERVICE.submit(() -> {
            uuidToRewardUser.put(uuid, storage.loadRewardUser(uuid));
            new BukkitRunnable() {
                @Override
                public void run() {
                    onComplete.run();
                }
            }.runTask(ActivityRewarder.getInstance());
        });
    }

    public void saveRewardUser(RewardUser rewardUser) {
        Storage.SERVICE.submit(() -> storage.saveRewardUser(rewardUser));
    }

    public RewardUser getRewardUser(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return null;
        return uuidToRewardUser.getOrDefault(uuid, new RewardUser(uuid, player.getName(), LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(), 1, (int) getTicksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE))));
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
