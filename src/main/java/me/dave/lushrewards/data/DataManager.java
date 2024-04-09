package me.dave.lushrewards.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.platyutils.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.IOHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DataManager extends Manager {
    private IOHandler<StorageData, StorageLocation> ioHandler;
    private final ConcurrentHashMap<UUID, RewardUser> rewardUsersCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        ConfigManager configManager = LushRewards.getInstance().getConfigManager();
        ioHandler = new IOHandler<>(configManager.getStorage());

        Bukkit.getOnlinePlayers().forEach(player -> getOrLoadRewardUser(player).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName())));
    }

    @Override
    public void onDisable() {
        if (ioHandler != null) {
            saveCachedRewardUsers();
            ioHandler.disableIOHandler();
            ioHandler = null;
        }
    }

    @Nullable
    public RewardUser getRewardUser(@NotNull Player player) {
        return rewardUsersCache.get(player.getUniqueId());
    }

    public boolean isRewardUserLoaded(UUID uuid) {
        return rewardUsersCache.containsKey(uuid);
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        CompletableFuture<RewardUser> future = ioHandler.loadData(uuid);

        future.thenAccept((rewardUser) -> {
            rewardUsersCache.put(uuid, rewardUser);
            loadModulesUserData(uuid);
        });

        return future;
    }

    @NotNull
    public CompletableFuture<RewardUser> getOrLoadRewardUser(@NotNull Player player) {
        CompletableFuture<RewardUser> completableFuture = new CompletableFuture<>();

        UUID uuid = player.getUniqueId();
        RewardUser rewardUser = rewardUsersCache.get(uuid);
        if (rewardUser != null) {
            completableFuture.complete(rewardUser);
        } else {
            loadRewardUser(uuid).thenAccept(completableFuture::complete);
        }

        return completableFuture;
    }

    @NotNull
    public CompletableFuture<RewardUser> getOrTempLoadRewardUser(@NotNull Player player) {
        CompletableFuture<RewardUser> completableFuture = new CompletableFuture<>();

        UUID uuid = player.getUniqueId();
        RewardUser rewardUser = rewardUsersCache.get(uuid);
        if (rewardUser != null) {
            completableFuture.complete(rewardUser);
        } else {
            ioHandler.loadData(uuid).thenAccept(completableFuture::complete);
        }

        return completableFuture;
    }

    public void unloadRewarderUser(UUID uuid) {
        rewardUsersCache.remove(uuid);
        unloadModulesUserData(uuid);
    }

    public void reloadRewardUsers() {
        rewardUsersCache.forEach((uuid, rewardUser) -> {
            saveRewardUser(rewardUser);
            unloadRewarderUser(uuid);
            loadRewardUser(uuid);
        });
    }

    public void saveRewardUser(Player player) {
        RewardUser rewardUser = getRewardUser(player);
        if (rewardUser != null) {
            saveRewardUser(rewardUser);
        }
    }

    public void saveRewardUser(RewardUser rewardUser) {
        ioHandler.saveData(rewardUser);
        saveModulesUserData(rewardUser.getUniqueId());
    }

    public void saveCachedRewardUsers() {
        rewardUsersCache.values().forEach(this::saveRewardUser);
    }

    public void loadModulesUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                UserDataModule.UserData userData = userDataModule.getUserData(uuid).fromJson(null);
                userDataModule.cacheUserData(uuid, userData != null ? userData : userDataModule.getDefaultData(uuid));
            }
        });
    }

    public void unloadModulesUserData(UUID uuid) {
        LushRewards.getInstance().getRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                userDataModule.uncacheUserData(uuid);
            }
        });
    }

    public void saveModuleUserData(UUID uuid, String moduleId) {
        LushRewards.getInstance().getModule(moduleId).ifPresent(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                JsonElement moduleJson = userDataModule.getUserData(uuid).asJson();

            }
        });
    }

    public void saveModulesUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                JsonElement moduleJson = userDataModule.getUserData(uuid).asJson();

            }
        });
    }

    private boolean isOutdated() {
        File playerDataFile = new File(LushRewards.getInstance().getDataFolder(), "data");
        if (playerDataFile.exists()) {
            File[] dataFiles = playerDataFile.listFiles();

            if (dataFiles != null && dataFiles.length > 0) {
                File dataFile = dataFiles[0];
                YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

                return data.contains("dailyrewards.day-num", true);
            }
        }

        return false;
    }

    public record StorageLocation(@NotNull UUID uuid, @Nullable String moduleId) {}
    public record StorageData(@NotNull UUID uuid, @Nullable String moduleId, @NotNull JsonObject json) {}
}
