package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.api.event.RewardUserLoadEvent;
import me.dave.activityrewarder.api.event.RewardUserUnloadEvent;
import me.dave.activityrewarder.importer.ActivityRewarderDataUpdater;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModuleUserData;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.IOHandler;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager {
    private final IOHandler<RewardUser, UUID> ioHandler = new IOHandler<>(new YmlStorage());
    private final ConcurrentHashMap<UUID, RewardUser> uuidToRewardUser = new ConcurrentHashMap<>();

    public DataManager() {
        if (isOutdated()) {
            try {
                new ActivityRewarderDataUpdater().startImport();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Bukkit.getOnlinePlayers().forEach(player -> getOrLoadRewardUser(player).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName())));
    }

    public void reloadRewardUsers() {
        uuidToRewardUser.forEach((uuid, rewardUser) -> {
            rewardUser.save();
            unloadRewarderUser(uuid);
            loadRewardUser(uuid);
        });
    }

    @NotNull
    public CompletableFuture<RewardUser> getOrLoadRewardUser(@NotNull Player player) {
        CompletableFuture<RewardUser> completableFuture = new CompletableFuture<>();

        UUID uuid = player.getUniqueId();
        RewardUser rewardUser = uuidToRewardUser.get(uuid);
        if (rewardUser != null) {
            completableFuture.complete(rewardUser);
        } else {
            loadRewardUser(uuid).thenAccept(completableFuture::complete);
        }

        return completableFuture;
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        return ioHandler.loadData(uuid).thenApply((rewardUser) -> {
            uuidToRewardUser.put(uuid, rewardUser);
            ActivityRewarder.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> ActivityRewarder.getInstance().callEvent(new RewardUserLoadEvent(rewardUser)));
            return rewardUser;
        });
    }

    public void unloadRewarderUser(UUID uuid) {
        RewardUser rewardUser = uuidToRewardUser.get(uuid);

        if (rewardUser != null && ActivityRewarder.getInstance().callEvent(new RewardUserUnloadEvent(rewardUser))) {
            uuidToRewardUser.remove(uuid);
        }
    }

    public void saveRewardUser(Player player) {
        ioHandler.saveData(getRewardUser(player));
    }

    public void saveRewardUser(RewardUser rewardUser) {
        ioHandler.saveData(rewardUser);
    }

    public void saveAll() {
        uuidToRewardUser.values().forEach(this::saveRewardUser);
    }

    @NotNull
    public RewardUser getRewardUser(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        RewardUser rewardUser = uuidToRewardUser.get(uuid);
        if (rewardUser == null) {
            rewardUser = new RewardUser(uuid, player.getName(), 0);

            if (ActivityRewarder.getInstance().getModule(DailyRewardsModule.ID).isPresent()) {
                rewardUser.addModuleData(new DailyRewardsModuleUserData(DailyRewardsModule.ID, 0, 0, LocalDate.now(), null, new HashSet<>()));
            }

            if (ActivityRewarder.getInstance().getModule(PlaytimeDailyGoalsModule.ID).isPresent()) {
                rewardUser.addModuleData(new PlaytimeDailyGoalsModuleUserData(PlaytimeDailyGoalsModule.ID, 0, LocalDate.now(), 0));
            }

            if (ActivityRewarder.getInstance().getModule(PlaytimeGlobalGoalsModule.ID).isPresent()) {
                rewardUser.addModuleData(new PlaytimeGoalsModuleUserData(PlaytimeGlobalGoalsModule.ID, 0));
            }
        }

        return rewardUser;
    }

    public boolean isRewardUserLoaded(UUID uuid) {
        return uuidToRewardUser.containsKey(uuid);
    }

    public IOHandler<RewardUser, UUID> getIoHandler() {
        return ioHandler;
    }

    private boolean isOutdated() {
        File playerDataFile = new File(ActivityRewarder.getInstance().getDataFolder(), "data");
        if (playerDataFile.exists()) {
            File[] dataFiles = playerDataFile.listFiles();

            if (dataFiles != null && dataFiles.length > 0) {
                File dataFile = dataFiles[0];
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

                return data.contains("hoursPlayed", true) && !data.contains("minutes-played", true);
            }
        }

        return false;
    }
}
