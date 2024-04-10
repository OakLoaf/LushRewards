package me.dave.lushrewards.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.platyutils.manager.Manager;
import me.dave.platyutils.module.Module;
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

        Bukkit.getOnlinePlayers().forEach(player -> getOrLoadRewardUser(player.getUniqueId()).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName())));
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

    public CompletableFuture<RewardUser> getOrLoadRewardUser(UUID uuid) {
        return getOrLoadRewardUser(uuid, true);
    }

    public CompletableFuture<RewardUser> getOrLoadRewardUser(UUID uuid, boolean cacheUser) {
        if (rewardUsersCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(rewardUsersCache.get(uuid));
        } else {
            return loadRewardUser(uuid, cacheUser);
        }
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        return loadRewardUser(uuid, true);
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid, boolean cacheUser) {
        CompletableFuture<RewardUser> future = loadUserData(uuid, null, RewardUser.class);
        if (cacheUser) {
            future.thenAccept(rewardUser -> rewardUsersCache.put(uuid, rewardUser));
        }
        return future;
    }

    public void unloadRewarderUser(UUID uuid) {
        rewardUsersCache.remove(uuid);
        unloadModulesUserData(uuid);
    }

    /**
     * Reload all cached RewardUsers
     * @param save Whether cached RewardUsers should be saved before reloading
     */
    public void reloadRewardUsers(boolean save) {
        rewardUsersCache.forEach((uuid, rewardUser) -> {
            if (save) {
                saveRewardUser(rewardUser);
            }

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

    public void saveCachedRewardUsers() {
        rewardUsersCache.values().forEach(this::saveRewardUser);
    }

    public void saveRewardUser(RewardUser rewardUser) {
        saveUserData(rewardUser);
        saveModulesUserData(rewardUser.getUniqueId());
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> getOrLoadUserData(UUID uuid, String module, Class<T> dataClass) {
        return getOrLoadUserData(uuid, module, dataClass, true);
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> getOrLoadUserData(UUID uuid, String moduleId, UserDataModule<T> module, Class<T> dataClass, boolean cacheUser) {
        // TODO: Make this actually compile and work
        T userData = module.getUserData(uuid);
        if (userData != null) {
            return CompletableFuture.completedFuture(userData);
        } else {
            loadUserData(uuid, userData.getModuleId(), dataClass);
            userDataModule.cacheUserData(uuid, userData);
            return
        }
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadUserData(UUID uuid, String module, Class<T> dataClass) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ioHandler.loadData(new StorageLocation(uuid, module)).thenAccept(storageData -> future.complete(new Gson().fromJson(storageData.json(), dataClass)));
        return future;
    }

    public <T extends UserDataModule.UserData> void saveUserData(T userData) {
        ioHandler.saveData(new StorageData(userData.getUniqueId(), userData.getModuleId(), userData.asJson()));
    }

    public void loadModulesUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                UserDataModule.UserData userData = userDataModule.getUserData(uuid);
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

    public void saveModulesUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                saveModuleUserData(uuid, userDataModule);
            }
        });
    }

    public void saveModuleUserData(UUID uuid, String moduleId) {
        LushRewards.getInstance().getModule(moduleId).ifPresent(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                saveModuleUserData(uuid, userDataModule);
            }
        });
    }

    public void saveModuleUserData(UUID uuid, UserDataModule<?> userDataModule) {
        UserDataModule.UserData userData = userDataModule.getUserData(uuid);
        if (userData != null) {
            saveUserData(userData);
        }
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
    public record StorageData(@NotNull UUID uuid, @Nullable String moduleId, @Nullable JsonObject json) {}
}
