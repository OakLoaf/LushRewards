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
        this.storage = switch (storageType) {
            case "mysql", "mariadb" -> new MySQLStorage();
            case "postgres" -> new PostgreSQLStorage();
            case "sqlite" -> new SQLiteStorage();
            case "json" -> new JsonStorage();
            default -> {
                LushRewards.getInstance().getLogger().severe("'%s' is not a valid storage type, defaulting to json storage."
                    .formatted(storageType));
                yield new JsonStorage();
            }
        };

        LushRewards.getInstance().getLogger().info("Setting up '%s' database".formatted(storageType));

        ConfigurationSection storageSection;
        if (config.contains("mysql")) {
            storageSection = config.getConfigurationSection("mysql");
            LushRewards.getInstance().getLogger().warning("Deprecated: The 'mysql' section in the storage.yml has been renamed to 'storage'");
        } else {
            storageSection = config.getConfigurationSection("storage");
        }

        this.storage.enable(storageSection);
    }

    public CompletableFuture<Void> disable() {
        if (this.storage != null) {
            return runAsync(this.storage::disable);
        } else {
            return CompletableFuture.completedFuture(null);
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
