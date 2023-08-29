package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import me.dave.activityrewarder.module.playtimegoals.PlaytimeGoalsModuleUserData;
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
            List<String> collectedDates = configurationSection.getStringList("daily-rewards.collected-dates");

            rewardUser.addModuleData(new DailyRewardsModuleUserData("daily-rewards", dayNum, highestStreak, SimpleDate.parse(startDate), SimpleDate.parse(lastCollectedDate), collectedDates));
        }

        if (ActivityRewarder.getModule("daily-playtime-goals") != null) {
            int lastCollectedPlaytime = configurationSection.getInt("daily-playtime-goals.last-collected-playtime", 0);
            List<Integer> collectedTimes = configurationSection.getIntegerList("daily-playtime-goals.collected-times");

            rewardUser.addModuleData(new PlaytimeGoalsModuleUserData("daily-playtime-goals", lastCollectedPlaytime, collectedTimes));
        }

        if (ActivityRewarder.getModule("global-playtime-goals") != null) {
            int lastCollectedPlaytime = configurationSection.getInt("global-playtime-goals.last-collected-playtime", 0);
            List<Integer> collectedTimes = configurationSection.getIntegerList("global-playtime-goals.collected-times");

            rewardUser.addModuleData(new PlaytimeGoalsModuleUserData("global-playtime-goals", lastCollectedPlaytime, collectedTimes));
        }

        return rewardUser;
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());

        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());

        if (rewardUser.getModuleData("daily-rewards") instanceof DailyRewardsModuleUserData dailyRewardsModuleData) {
            configurationSection.set("daily-rewards.day-num", dailyRewardsModuleData.getDayNum());
            configurationSection.set("daily-rewards.highest-streak", dailyRewardsModuleData.getHighestStreak());
            configurationSection.set("daily-rewards.start-date", dailyRewardsModuleData.getStartDate().toString("dd-mm-yyyy"));
            configurationSection.set("daily-rewards.last-collected-date", dailyRewardsModuleData.getLastCollectedDate().toString("dd-mm-yyyy"));
            configurationSection.set("daily-rewards.collected-dates", dailyRewardsModuleData.getCollectedDates());
        }

        if (rewardUser.getModuleData("daily-playtime-goals") instanceof PlaytimeGoalsModuleUserData dailyPlaytimeGoalsModuleUserData) {
            configurationSection.set("daily-playtime-goals.last-collected-playtime", dailyPlaytimeGoalsModuleUserData.getLastCollectedPlaytime());
            configurationSection.set("daily-playtime-goals.collected-times", dailyPlaytimeGoalsModuleUserData.getCollectedTimes());
        }

        if (rewardUser.getModuleData("global-playtime-goals") instanceof PlaytimeGoalsModuleUserData globalPlaytimeGoalsModuleUserData) {
            configurationSection.set("global-playtime-goals.last-collected-playtime", globalPlaytimeGoalsModuleUserData.getLastCollectedPlaytime());
            configurationSection.set("global-playtime-goals.last-collected-times", globalPlaytimeGoalsModuleUserData.getCollectedTimes());
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
