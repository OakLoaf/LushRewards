package me.dave.activityrewarder.data;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.IOHandler;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DataManager {
    private final IOHandler<RewardUser> ioHandler = new IOHandler<>(new YmlStorage());
    private final HashMap<UUID, RewardUser> uuidToRewardUser = new HashMap<>();

    public DataManager() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadRewardUser(player.getUniqueId()).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName()));
        }
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        return ioHandler.loadPlayer(uuid).thenApply((rewardUser) -> {
            uuidToRewardUser.put(uuid, rewardUser);
            return rewardUser;
        });
    }

    public void unloadRewarderUser(UUID uuid) {
        uuidToRewardUser.remove(uuid);
    }

    public void saveRewardUser(RewardUser rewardUser) {
        ioHandler.savePlayer(rewardUser);
    }

    @NotNull
    public RewardUser getRewardUser(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        RewardUser rewardUser = uuidToRewardUser.get(uuid);
        if (rewardUser == null) rewardUser = new RewardUser(uuid, player.getName(), LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(), 1, 1, (int) getTicksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE)));
        return rewardUser;
    }

    public boolean isRewardUserLoaded(UUID uuid) {
        return uuidToRewardUser.containsKey(uuid);
    }

    public IOHandler<RewardUser> getIoHandler() {
        return ioHandler;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
