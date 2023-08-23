package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

public class YmlStorage implements Storage<RewardUser> {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final File dataFolder = new File(plugin.getDataFolder(), "data");

    @Override
    public RewardUser load(UUID uuid) {
        ConfigurationSection configurationSection = loadOrCreateFile(uuid);
        String name = configurationSection.getString("name");
        String startDate = configurationSection.getString("startDate");
        String lastCollectedDate = configurationSection.getString("lastCollectedDate");
        int dayNum = configurationSection.getInt("dayNum", 1);
        int highestStreak = configurationSection.getInt("highestStreak", 1);
        int playTime = configurationSection.getInt("minutesPlayed", 0);
        return new RewardUser(uuid, name, startDate, lastCollectedDate, dayNum, highestStreak, playTime);
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration yamlConfiguration = loadOrCreateFile(rewardUser.getUUID());
        yamlConfiguration.set("name", rewardUser.getUsername());
        yamlConfiguration.set("startDate", rewardUser.getStartDate().toString());
        yamlConfiguration.set("lastCollectedDate", rewardUser.getLastDate().toString());
        yamlConfiguration.set("dayNum", rewardUser.getDayNum());
        yamlConfiguration.set("minutesPlayed", rewardUser.getPlayMinutes());
        File file = new File(dataFolder, rewardUser.getUUID().toString());
        try {
            yamlConfiguration.save(file);
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    public YamlConfiguration loadOrCreateFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString());
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        if (yamlConfiguration.getString("name") == null) {
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player.getName();
            yamlConfiguration.set("name", playerName);
            yamlConfiguration.set("startDate", LocalDate.now().toString());
            yamlConfiguration.set("lastCollectedDate", LocalDate.now().minusDays(1).toString());
            yamlConfiguration.set("dayNum", 1);
            yamlConfiguration.set("minutesPlayed", 0);
            try {
                yamlConfiguration.save(file);
            } catch(IOException err) {
                err.printStackTrace();
            }
        }
        return yamlConfiguration;
    }
}
