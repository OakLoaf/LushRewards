package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.IOHandler;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private final IOHandler<RewardUser> ioHandler = new IOHandler<>(new YmlStorage());
    private final HashMap<UUID, RewardUser> uuidToRewardUser = new HashMap<>();

    public CompletableFuture<Void> loadRewardUser(UUID uuid) {
        return ioHandler.loadPlayer(uuid).thenAccept((rewardUser) -> uuidToRewardUser.put(uuid, rewardUser));
    }

    public void saveRewardUser(RewardUser rewardUser) {
        ioHandler.savePlayer(rewardUser);
    }

    public RewardUser getRewardUser(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return null;
        return uuidToRewardUser.getOrDefault(uuid, new RewardUser(uuid, player.getName(), LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(), 1, (int) getTicksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE))));
    }

    public IOHandler<RewardUser> getIoHandler() {
        return ioHandler;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
