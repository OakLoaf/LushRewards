package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.activityrewarder.ActivityRewarder;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class YmlStorage implements Storage {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private File dataFile;
    private YamlConfiguration config;
    private final ReentrantLock fileLock = new ReentrantLock();

    @Override
    public RewardUser loadRewardUser(UUID uuid) {
        ConfigurationSection configurationSection = config.getConfigurationSection(uuid.toString());
        if (configurationSection == null) {
            configurationSection = config.createSection(uuid.toString());
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player.getName();
            long hoursPlayed = getTicksToHours(player.getStatistic(Statistic.PLAY_ONE_MINUTE));
            configurationSection.set("name", playerName);
            configurationSection.set("startDate", LocalDate.now().toString());
            configurationSection.set("latestCollectedDate", LocalDate.now().minusDays(1).toString());
            configurationSection.set("dayNum", 0);
            configurationSection.set("hoursPlayed", 0);
            plugin.saveConfig();
            return new RewardUser(uuid, player.getName(), LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(), 1, (int) hoursPlayed);
        }
        String name = configurationSection.getString("name");
        String startDate = configurationSection.getString("startDate");
        String latestCollectedDate = configurationSection.getString("latestCollectedDate");
        int dayNum = configurationSection.getInt("dayNum", 0);
        int playTime = configurationSection.getInt("hoursPlayed", 0);
        return new RewardUser(uuid, name, startDate, latestCollectedDate, dayNum, playTime);
    }

    @Override
    public void saveRewardUser(RewardUser rewardUser) {
        fileLock.lock();
        ConfigurationSection configurationSection = config.createSection(rewardUser.getUUID().toString());
        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("startDate", rewardUser.getStartDate().toString());
        configurationSection.set("latestCollectedDate", rewardUser.getLatestDate().toString());
        configurationSection.set("dayNum", rewardUser.getDayNum());
        configurationSection.set("hoursPlayed", rewardUser.getPlayTime());
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fileLock.unlock();
        }
    }

    @Override
    public boolean init() {
        File dataFile = new File(plugin.getDataFolder(),"data.yml");
        try {
            if (dataFile.createNewFile()) plugin.getLogger().info("File Created: data.yml");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        this.dataFile = dataFile;
        config = YamlConfiguration.loadConfiguration(dataFile);
        return true;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
