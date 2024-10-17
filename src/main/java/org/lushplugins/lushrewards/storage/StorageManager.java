package org.lushplugins.lushrewards.storage;

import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.lushplugins.lushrewards.storage.type.JsonStorage;
import org.lushplugins.lushrewards.storage.type.MySQLStorage;
import org.lushplugins.lushrewards.storage.type.PostgreSQLStorage;
import org.lushplugins.lushrewards.storage.type.SQLiteStorage;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

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
            case "mysql", "mariadb" -> storage = new MySQLStorage();
            case "postgres" -> storage = new PostgreSQLStorage();
            case "sqlite" -> storage = new SQLiteStorage();
            case "json" -> storage = new JsonStorage();
            default -> {
                storage = new JsonStorage();
                LushRewards.getInstance().getLogger().severe("'" + storageType + "' is not a valid storage type, default to json storage.");
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

    public CompletableFuture<JsonObject> loadModuleUserData(UUID uuid, String moduleId) {
        return runAsync(() -> storage.loadModuleUserDataJson(uuid, moduleId));
    }

    public CompletableFuture<Void> saveModuleUserData(UserDataModule.UserData userData) {
        return runAsync(() -> storage.saveModuleUserData(userData));
    }

    private <T> CompletableFuture<T> runAsync(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        threads.submit(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable e) {
                LushRewards.getInstance().getLogger().log(Level.WARNING, "Caught unhandled storage error: ", e);
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
                LushRewards.getInstance().getLogger().log(Level.WARNING, "Caught unhandled storage error: ", e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
