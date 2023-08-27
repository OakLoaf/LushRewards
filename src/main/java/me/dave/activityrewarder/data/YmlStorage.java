package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleData;
import me.dave.activityrewarder.module.playtimegoals.PlaytimeGoalsModuleData;
import me.dave.activityrewarder.utils.SimpleDate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class YmlStorage implements Storage<RewardUser> {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final File dataFolder = new File(plugin.getDataFolder(), "data");

    @Override
    public RewardUser load(UUID uuid) {
        ConfigurationSection configurationSection = loadOrCreateFile(uuid);

        String name = configurationSection.getString("name");
        int minutesPlayed = configurationSection.getInt("minutes-played", 0);

        RewardUser rewardUser = new RewardUser(uuid, name, minutesPlayed);

        if (ActivityRewarder.getModule("daily-rewards") != null) {
            int dayNum = configurationSection.getInt("daily-rewards.day-num", 1);
            int highestStreak = configurationSection.getInt("daily-rewards.highest-streak", 1);
            String startDate = configurationSection.getString("daily-rewards.start-date", SimpleDate.now().toString("dd-mm-yyyy"));
            String lastCollectedDate = configurationSection.getString("daily-rewards.last-collected-date", SimpleDate.now().toString("dd-mm-yyyy"));

            rewardUser.addModuleData(new DailyRewardsModuleData("daily-rewards", dayNum, highestStreak, SimpleDate.parse(startDate), SimpleDate.parse(lastCollectedDate)));
        }

        if (ActivityRewarder.getModule("daily-playtime-goals") != null) {
            int lastCollectedPlaytime = configurationSection.getInt("daily-playtime-goals.last-collected-playtime", 0);
            List<Integer> collectedTimes = configurationSection.getIntegerList("daily-playtime-goals.collected-times");

            rewardUser.addModuleData(new PlaytimeGoalsModuleData("daily-playtime-goals", lastCollectedPlaytime, collectedTimes));
        }

        if (ActivityRewarder.getModule("global-playtime-goals") != null) {
            int lastCollectedPlaytime = configurationSection.getInt("global-playtime-goals.last-collected-playtime", 0);
            List<Integer> collectedTimes = configurationSection.getIntegerList("global-playtime-goals.collected-times");

            rewardUser.addModuleData(new PlaytimeGoalsModuleData("global-playtime-goals", lastCollectedPlaytime, collectedTimes));
        }

        return rewardUser;
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());

        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());

        DailyRewardsModuleData dailyRewardsModuleData = rewardUser.getDailyRewardsModuleData();
        if (dailyRewardsModuleData != null) {
            configurationSection.set("daily-rewards.day-num", dailyRewardsModuleData.getDayNum());
            configurationSection.set("daily-rewards.highest-streak", dailyRewardsModuleData.getHighestStreak());
            configurationSection.set("daily-rewards.start-date", dailyRewardsModuleData.getStartDate().toString("dd-mm-yyyy"));
            configurationSection.set("daily-rewards.last-collected-date", dailyRewardsModuleData.getLastCollectedDate().toString("dd-mm-yyyy"));
        }

        PlaytimeGoalsModuleData dailyPlaytimeGoalsModuleData = rewardUser.getDailyPlaytimeGoalsModuleData();
        if (dailyPlaytimeGoalsModuleData != null) {
            configurationSection.set("daily-playtime-goals.last-collected-playtime", dailyPlaytimeGoalsModuleData.getLastCollectedPlaytime());
            configurationSection.set("daily-playtime-goals.last-collected-playtime", dailyPlaytimeGoalsModuleData.getCollectedTimes());
        }

        PlaytimeGoalsModuleData globalPlaytimeGoalsModuleData = rewardUser.getGlobalPlaytimeGoalsModuleData();
        if (globalPlaytimeGoalsModuleData != null) {
            configurationSection.set("global-playtime-goals.last-collected-playtime", globalPlaytimeGoalsModuleData.getLastCollectedPlaytime());
            configurationSection.set("global-playtime-goals.last-collected-playtime", globalPlaytimeGoalsModuleData.getCollectedTimes());
        }

        File file = new File(dataFolder, rewardUser.getUniqueId().toString());
        try {
            configurationSection.save(file);
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    private YamlConfiguration loadOrCreateFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString());
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);

        if (yamlConfiguration.getString("name") == null) {
            Player player = Bukkit.getPlayer(uuid);
            String playerName = player.getName();
            yamlConfiguration.set("name", playerName);
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
