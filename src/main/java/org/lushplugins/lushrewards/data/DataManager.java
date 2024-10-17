package org.lushplugins.lushrewards.data;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.lushplugins.lushlib.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.storage.StorageManager;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DataManager extends Manager {
    private StorageManager storageManager;
    private final ConcurrentHashMap<UUID, RewardUser> rewardUsersCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        storageManager = new StorageManager();

        Bukkit.getOnlinePlayers().forEach(player -> getOrLoadRewardUser(player.getUniqueId()).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName())));
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            saveCachedRewardUsers();
            storageManager.disable();
            storageManager = null;
        }
    }

    @Nullable
    public RewardUser getRewardUser(@NotNull Player player) {
        return getRewardUser(player.getUniqueId());
    }

    @Nullable
    public RewardUser getRewardUser(@NotNull UUID uuid) {
        return rewardUsersCache.get(uuid);
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

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<RewardUser> loadRewardUser(@NotNull UUID uuid) {
        return loadRewardUser(uuid, true);
    }

    public CompletableFuture<RewardUser> loadRewardUser(@NotNull UUID uuid, boolean cacheUser) {
        CompletableFuture<RewardUser> future = new CompletableFuture<>();

        loadUserData(uuid, null, RewardUser.class).thenAccept(rewardUser -> {
            if (rewardUser != null && cacheUser) {
                rewardUsersCache.put(uuid, rewardUser);
            }

            future.complete(rewardUser);
        });

        return future;
    }

    public void unloadRewardUser(UUID uuid) {
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

            unloadRewardUser(uuid);
            loadRewardUser(uuid);
        });
    }

    public void saveCachedRewardUsers() {
        rewardUsersCache.values().forEach(this::saveRewardUser);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CompletableFuture<Boolean> saveRewardUser(RewardUser rewardUser) {
        return saveUserData(rewardUser);
    }

    public void saveRewardUser(Player player) {
        RewardUser rewardUser = getRewardUser(player);
        if (rewardUser != null) {
            saveRewardUser(rewardUser);
        }
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
        CompletableFuture<T> future = new CompletableFuture<>();

        loadUserData(uuid, module.getId(), module.getUserDataClass()).thenAccept(userData -> {
            if (userData != null && cacheUser) {
                module.cacheUserData(uuid, userData);
            }

            future.complete(userData);
        });

        return future;
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadUserData(@NotNull UUID uuid, String moduleId, Class<T> dataClass) {
        CompletableFuture<T> future = new CompletableFuture<>();

        storageManager.loadModuleUserData(uuid, moduleId)
            .orTimeout(15, TimeUnit.SECONDS)
            .whenComplete((json, exception) -> {
                if (exception != null) {
                    LushRewards.getInstance().log(Level.WARNING, "Caught error when parsing data:", exception);
                    future.complete(null);
                    return;
                }

                if (json == null) {
                    LushRewards.getInstance().getLogger().info("No storage data found for '" + uuid + "' for module '" + (moduleId != null ? moduleId : "main") + "', creating default data!");

                    if (moduleId != null) {
                        UserDataModule<?> module = (UserDataModule<?>) LushRewards.getInstance().getModule(moduleId).orElse(null);
                        if (module != null) {
                            T userData = dataClass.cast(module.getDefaultData(uuid));
                            saveUserData(userData).thenAccept((ignored) -> future.complete(userData));
                        } else {
                            future.complete(null);
                        }
                    } else if (dataClass.isAssignableFrom(RewardUser.class)) {
                        T userData = dataClass.cast(new RewardUser(uuid, null, 0));
                        saveUserData(userData).thenAccept((ignored) -> future.complete(userData));
                    } else {
                        future.complete(null);
                    }

                    return;
                }

                try {
                    json.addProperty("uuid", uuid.toString());
                    json.addProperty("moduleId", moduleId);

                    T userData = LushRewards.getInstance().getGson().fromJson(json, dataClass);
                    if (userData == null) {
                        future.complete(null);
                        return;
                    }

                    if (userData instanceof PlaytimeRewardsModule.UserData playtimeUserData) {
                        PlaytimeRewardsModule module = (PlaytimeRewardsModule) LushRewards.getInstance().getModule(moduleId).orElse(null);
                        if (module != null) {
                            int resetPlaytimeAt = module.getResetPlaytimeAt();
                            if (resetPlaytimeAt > 0 && !playtimeUserData.getStartDate().isAfter(LocalDate.now().minusDays(resetPlaytimeAt))) {
                                playtimeUserData.setStartDate(LocalDate.now());
                                playtimeUserData.setPreviousDayEndPlaytime(playtimeUserData.getLastCollectedPlaytime());
                                saveUserData(userData);
                            }
                        }
                    }

                    future.complete(userData);
                } catch (Throwable e) {
                    LushRewards.getInstance().log(Level.WARNING, "Caught error when parsing user data:", e);
                    future.complete(null);
                }
            });

        return future;
    }

    public CompletableFuture<Boolean> saveUserData(UserDataModule.UserData userData) {
        return CompletableFuture.supplyAsync(() -> storageManager.saveModuleUserData(userData))
            .orTimeout(30, TimeUnit.SECONDS)
            .handle((storageData, exception) -> {
                if (exception != null) {
                    LushRewards.getInstance().log(Level.WARNING, "Caught error when saving data:", exception);
                    return false;
                }

                return storageData != null;
            });
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
            return saveUserData(userData);
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }
}
