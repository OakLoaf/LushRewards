package org.lushplugins.lushrewards.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.lushplugins.lushrewards.storage.type.SQLStorage;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageManager {
    private final ExecutorService threads = Executors.newFixedThreadPool(1);
    private Storage storage;

    public StorageManager() {
        reload();
    }

    public void reload() {
        disable();

        FileConfiguration config = LushRewards.getInstance().getConfigResource("storage.yml");

        String storageType = config.getString("type", "null");
        switch (config.getString("type", "null")) {
            case "mysql", "mariadb" -> storage = new SQLStorage();
//            case "postgres" -> storage = ...
//            case "sqlite" -> storage = ...
//            case "json" -> storage = ...
            default -> {
//                storage = ...
                LushRewards.getInstance().getLogger().severe("'" + storageType + "' is not a valid storage type, default to json");
            }
        }

        boolean outdated = config.contains("mysql");
        if (outdated) {
            LushRewards.getInstance().getLogger().warning("Deprecated: The 'mysql' section in the storage.yml has been renamed to 'storage'");
        }

        LushRewards.getInstance().getLogger().info("Setting up '" + storageType +"' database");
        ConfigurationSection storageSection = outdated ? config.getConfigurationSection("mysql") : config.getConfigurationSection("storage");
        storage.enable(storageSection);
    }

    public void disable() {
        if (storage != null) {
            storage.disable();
            storage = null;
        }
    }

    public CompletableFuture<RewardUser> loadRewardUser(UUID uuid) {
        return runAsync(() -> storage.loadRewardUser(uuid));
    }

    public CompletableFuture<Void> saveRewardUser(RewardUser rewardUser) {
        return runAsync(() -> storage.saveRewardUser(rewardUser));
    }

    public <T extends UserDataModule.UserData> CompletableFuture<T> loadModuleUserData(UUID uuid, UserDataModule<T> module) {
        return runAsync(() -> storage.loadModuleUserData(uuid, module));
    }

    public <T extends UserDataModule.UserData> CompletableFuture<Void> saveModuleUserData(UUID uuid, UserDataModule<T> module) {
        return runAsync(() -> storage.saveModuleUserData(uuid, module));
    }

    private <T> CompletableFuture<T> runAsync(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        threads.submit(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Void> runAsync(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        threads.submit(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Throwable e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
