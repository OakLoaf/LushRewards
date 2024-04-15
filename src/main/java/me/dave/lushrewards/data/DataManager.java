package me.dave.lushrewards.data;

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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    public CompletableFuture<RewardUser> loadRewardUser(@NotNull UUID uuid) {
        return loadRewardUser(uuid, true);
    }

    public CompletableFuture<RewardUser> loadRewardUser(@NotNull UUID uuid, boolean cacheUser) {
        CompletableFuture<RewardUser> future = loadUserData(uuid, null, RewardUser.class);
        if (cacheUser) {
            future.thenAccept(rewardUser -> rewardUsersCache.put(uuid, rewardUser));
        }
        return future;
    }

    public void unloadRewarderUser(UUID uuid) {
        rewardUsersCache.remove(uuid);
    }

    /**
     * Reload all cached RewardUsers
     *
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

    public CompletableFuture<Boolean> saveRewardUser(RewardUser rewardUser) {
        // TODO: Move this call to only be in specific places (outside of this method)
        //saveModulesUserData(rewardUser.getUniqueId());

        return saveUserData(rewardUser.getUniqueId(), rewardUser);
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> getOrLoadUserData(UUID uuid, UserDataModule<T> module) {
        return getOrLoadUserData(uuid, module, true);
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> getOrLoadUserData(UUID uuid, UserDataModule<T> module, boolean cacheUser) {
        T userData = module.getUserData(uuid);
        if (userData != null) {
            return CompletableFuture.completedFuture(userData);
        } else {
            return loadUserData(uuid, module, cacheUser);
        }
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadUserData(UUID uuid, UserDataModule<T> module) {
        return loadUserData(uuid, module, true);
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadUserData(UUID uuid, UserDataModule<T> module, boolean cacheUser) {
        CompletableFuture<T> future = loadUserData(uuid, module.getId(), module.getUserDataClass());
        future.thenAccept(userData -> {
            if (userData == null) {
                userData = module.getDefaultData(uuid);
            }

            if (cacheUser) {
                module.cacheUserData(uuid, userData);
            }
        });

        return future;
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadUserData(@NotNull UUID uuid, String moduleId, Class<T> dataClass) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ioHandler.loadData(new StorageLocation(uuid, moduleId))
            .completeOnTimeout(null, 15, TimeUnit.SECONDS)
            .thenAccept(storageData -> {
                if (storageData == null) {
                    future.complete(null);
                    return;
                }

                try {
                    JsonObject json = storageData.json();
                    if (json != null) {
                        json.addProperty("uuid", uuid.toString());
                        json.addProperty("moduleId", moduleId);
                    }

                    T userData = LushRewards.getInstance().getGson().fromJson(json, dataClass);
                    if (userData == null) {
                        if (moduleId != null) {
                            UserDataModule<?> module = (UserDataModule<?>) LushRewards.getInstance().getModule(moduleId).orElse(null);
                            if (module != null) {
                                userData = dataClass.cast(module.getDefaultData(uuid));
                            }
                        } else if (dataClass.isAssignableFrom(RewardUser.class)) {
                            userData = dataClass.cast(new RewardUser(uuid, null, 0));
                        }
                    }

                    future.complete(userData);
                } catch (Throwable e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            })
            .exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            });

        return future;
    }

    public <T extends UserDataModule.UserData> CompletableFuture<Boolean> saveUserData(@NotNull UUID uuid, T userData) {
        return ioHandler.saveData(new StorageData(uuid, userData.getModuleId(), userData.asJson()))
            .completeOnTimeout(null, 30, TimeUnit.SECONDS)
            .exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            })
            .thenApply(Objects::nonNull);
    }

    public void loadModulesUserData(UUID uuid) {
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                userDataModule.getOrLoadUserData(uuid, true);
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

    public CompletableFuture<Boolean> saveModuleUserData(UUID uuid, UserDataModule<?> userDataModule) {
        UserDataModule.UserData userData = userDataModule.getUserData(uuid);
        if (userData != null) {
            return saveUserData(uuid, userData);
        } else {
            return CompletableFuture.completedFuture(true);
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

    public record StorageLocation(@NotNull UUID uuid, @Nullable String moduleId) {
    }

    public record StorageData(@NotNull UUID uuid, @Nullable String moduleId, @Nullable JsonObject json) {
    }
}
