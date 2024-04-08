package me.dave.lushrewards.data;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.lushrewards.storage.StorageManager;
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
    private IOHandler<RewardUser, UUID> ioHandler;
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

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        loadModuleUserData(uuid);

        return ioHandler.loadData(uuid).thenApply((rewardUser) -> {
            rewardUsersCache.put(uuid, rewardUser);
            return rewardUser;
        });
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
        unloadModuleUserData(uuid);
    }

    public void reloadRewardUsers() {
        rewardUsersCache.forEach((uuid, rewardUser) -> {
            saveRewardUser(rewardUser);
            unloadRewarderUser(uuid);
            loadRewardUser(uuid);
        });
    }

    public boolean isRewardUserLoaded(UUID uuid) {
        return rewardUsersCache.containsKey(uuid);
    }

    public void saveRewardUser(RewardUser rewardUser) {
        ioHandler.saveData(rewardUser);
        saveModuleUserData(rewardUser.getUniqueId());
    }

    public void saveRewardUser(Player player) {
        RewardUser rewardUser = getRewardUser(player);
        if (rewardUser != null) {
            ioHandler.saveData(rewardUser);
        }
    }

    public void saveCachedRewardUsers() {
        rewardUsersCache.values().forEach(this::saveRewardUser);
    }

    public void loadModuleUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                LushRewards.getInstance().getStorageManager().loadData(uuid.toString(), userDataModule.getStorageProviderName(), userDataModule.getUserDataClass()).thenAccept(userData -> {
                    userDataModule.cacheUserData(uuid, userData != null ? userData : userDataModule.getDefaultData(uuid));
                });
            }
        });
    }

    public void unloadModuleUserData(UUID uuid) {
        LushRewards.getInstance().getRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                userDataModule.uncacheUserData(uuid);
            }
        });
    }

    public void saveModuleUserData(UUID uuid, String moduleId) {
        LushRewards.getInstance().getModule(moduleId).ifPresent(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                StorageManager storageManager = LushRewards.getInstance().getStorageManager();
                storageManager.getStorageProvider(userDataModule.getStorageProviderName()).getOrLoadObject(uuid.toString()).thenAccept(storageManager::saveData);
            }
        });
    }

    public void saveModuleUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                StorageManager storageManager = LushRewards.getInstance().getStorageManager();
                storageManager.getStorageProvider(userDataModule.getStorageProviderName()).getOrLoadObject(uuid.toString()).thenAccept(storageManager::saveData);
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
}
