package me.dave.lushrewards.storage.type;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.storage.StorageManager;
import me.dave.lushrewards.storage.StorageObject;
import me.dave.lushrewards.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YmlStorage implements Storage<StorageObject, StorageManager.ProviderId> {
    private final File dataFolder = new File(LushRewards.getInstance().getDataFolder(), "data");

    @Override
    public StorageObject load(StorageManager.ProviderId providerId) {
        String key = providerId.key();
        YamlConfiguration configurationSection = loadOrCreateFile(key);

        String providerName = providerId.providerName();
        StorageObject storageObject = new StorageObject(key, providerName);
        StorageProvider<?> storageProvider = LushRewards.getInstance().getStorageManager().getStorageProvider(providerName);
        if (storageProvider != null) {
            if (providerName != null) {
                storageProvider.getMethodHolders().forEach((id, methodHolder) -> storageObject.set(id, configurationSection.get(providerName + "." + id), methodHolder.getConvertToLocalMethod()));
            } else {
                storageProvider.getMethodHolders().forEach((id, methodHolder) -> storageObject.set(id, configurationSection.get(id), methodHolder.getConvertToLocalMethod()));
            }
        }

        return storageObject;
    }

    @Override
    public void save(StorageObject storageObject) {
        String key = storageObject.getKey();
        YamlConfiguration configurationSection = loadOrCreateFile(key);

        String providerName = storageObject.getProviderName();
        if (providerName != null) {
            storageObject.getValues().forEach((id, value) -> configurationSection.set(providerName + "." + id, value.remoteValue()));
        } else {
            storageObject.getValues().forEach((id, value) -> configurationSection.set(id, value.remoteValue()));
        }


        File file = new File(dataFolder, key);
        try {
            configurationSection.save(file);
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    private YamlConfiguration loadOrCreateFile(String fileName) {
        File file = new File(dataFolder, fileName);
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);

        if (yamlConfiguration.getString("name") == null) {
            Player player = Bukkit.getPlayer(UUID.fromString(fileName));
            if (player != null) {
                String playerName = player.getName();
                yamlConfiguration.set("name", playerName);
            }

            yamlConfiguration.set("minutes-played", 0);

            try {
                yamlConfiguration.save(file);
            } catch(IOException err) {
                err.printStackTrace();
            }
        }

        return yamlConfiguration;
    }
}
