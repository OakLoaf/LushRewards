package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;
import org.enchantedskies.activityrewarder.ActivityRewarder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class YmlStorage implements Storage<RewardUser> {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final File dataFolder = new File(plugin.getDataFolder(), "data");

    public void init() {
        try {
            dataFolder.createNewFile();
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    @Override
    public RewardUser load(UUID uuid) {
        ConfigurationSection configurationSection = loadOrCreateFile(uuid);
        String name = configurationSection.getString("name");
        String startDate = configurationSection.getString("startDate");
        String lastCollectedDate = configurationSection.getString("lastCollectedDate");
        int dayNum = configurationSection.getInt("dayNum", 1);
        int playTime = configurationSection.getInt("hoursPlayed", 0);
        return new RewardUser(uuid, name, startDate, lastCollectedDate, dayNum, playTime);
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration yamlConfiguration = loadOrCreateFile(rewardUser.getUUID());
        yamlConfiguration.set("name", rewardUser.getUsername());
        yamlConfiguration.set("startDate", rewardUser.getStartDate().toString());
        yamlConfiguration.set("lastCollectedDate", rewardUser.getLastDate().toString());
        yamlConfiguration.set("dayNum", rewardUser.getDayNum());
        yamlConfiguration.set("hoursPlayed", rewardUser.getPlayTime());
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
            yamlConfiguration.set("hoursPlayed", (int) getTicksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE)));
            try {
                yamlConfiguration.save(file);
            } catch(IOException err) {
                err.printStackTrace();
            }
        }
        return yamlConfiguration;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }

}
