package me.dave.lushrewards.storage;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.utils.Keyed;
import me.dave.platyutils.manager.Manager;
import org.enchantedskies.EnchantedStorage.IOHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StorageManager extends Manager {
    private IOHandler<StorageObject, ProviderId> ioHandler;
    private HashMap<String, StorageProvider<?>> storageProviders;

    @Override
    public void onEnable() {
        ConfigManager configManager = LushRewards.getInstance().getConfigManager();
        ioHandler = new IOHandler<>(configManager.getModuleStorage());

        storageProviders = new HashMap<>();
    }

    @Override
    public void onDisable() {
        if (storageProviders != null) {
            storageProviders.clear();
            storageProviders = null;
        }
    }

    public CompletableFuture<StorageObject> loadData(String uniqueId, String providerName) {
        return ioHandler.loadData(new ProviderId(uniqueId, providerName));
    }

    public <T> CompletableFuture<T> loadData(String uniqueId, String providerName, Class<T> clazz) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();

        ioHandler.loadData(new ProviderId(uniqueId, providerName)).thenAccept(storageObject -> {
            try {
                Keyed data = storageProviders.get(providerName).convertObject(storageObject);
                if (data == null) {
                    completableFuture.complete(null);
                    return;
                }

                completableFuture.complete(clazz.cast(data));
            } catch (Throwable e) {
                LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to load data", e);
            }
        });

        return completableFuture;
    }

    public void saveData(StorageObject storageObject) {
        ioHandler.saveData(storageObject);
    }

    @Nullable
    public StorageProvider<?> getStorageProvider(String name) {
        return storageProviders.get(name);
    }

    public Collection<StorageProvider<?>> getStorageProviders() {
        return storageProviders.values();
    }

    public void registerStorageProvider(StorageProvider<?> storageProvider) {
        if (storageProviders.containsKey(storageProvider.getName())) {
            throw new IllegalStateException("A StorageProvider with this id has already been registered");
        }

        storageProviders.put(storageProvider.getName(), storageProvider);
    }

    public void unregisterStorageProvider(String id) {
        storageProviders.remove(id);
    }

    public record ProviderId(String key, String providerName) {}
}
