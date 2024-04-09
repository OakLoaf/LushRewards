package me.dave.lushrewards.data;

import me.dave.lushrewards.LushRewards;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YmlStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    private final File dataFolder = new File(LushRewards.getInstance().getDataFolder(), "data");

//    @Override
//    public RewardUser load(UUID uuid) {
//        ConfigurationSection configurationSection = loadOrCreateFile(uuid);
//
//        String name = configurationSection.getString("name");
//        int minutesPlayed = configurationSection.getInt("minutes-played", 0);
//
//        return new RewardUser(uuid, name, minutesPlayed);
//    }
//
//    @Override
//    public void save(RewardUser rewardUser) {
//        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());
//
//        configurationSection.set("name", rewardUser.getUsername());
//        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());
//
//        File file = new File(dataFolder, rewardUser.getUniqueId().toString());
//        try {
//            configurationSection.save(file);
//        } catch(IOException err) {
//            err.printStackTrace();
//        }
//    }

    @Override
    public DataManager.StorageData load(DataManager.StorageLocation storageLocation) {
        return null;
    }

    @Override
    public void save(DataManager.StorageData storageData) {

    }

    private YamlConfiguration loadOrCreateFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString());
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);

        if (yamlConfiguration.getString("name") == null) {
            Player player = Bukkit.getPlayer(uuid);
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
