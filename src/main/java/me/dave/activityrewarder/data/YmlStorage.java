package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModuleUserData;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

        if (ActivityRewarder.getModule(Module.ModuleType.DAILY_REWARDS.getName()) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(Module.ModuleType.DAILY_REWARDS.getName());

            if (moduleSection != null) {
                int streakLength = configurationSection.getInt("streak-length", 1);
                int highestStreak = configurationSection.getInt("highest-streak", 1);
                String startDate = configurationSection.getString("start-date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                String lastCollectedDate = configurationSection.getString("last-collected-date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                List<String> collectedDates = configurationSection.getStringList("collected-dates");

                rewardUser.addModuleData(new DailyRewardsModuleUserData(Module.ModuleType.DAILY_REWARDS.getName(), streakLength, highestStreak, LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd-MM-yyyy")), LocalDate.parse(lastCollectedDate, DateTimeFormatter.ofPattern("dd-MM-yyyy")), collectedDates));
            }
        }

        if (ActivityRewarder.getModule(Module.ModuleType.DAILY_PLAYTIME_GOALS.getName()) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(Module.ModuleType.DAILY_PLAYTIME_GOALS.getName());

            if (moduleSection != null) {
                String date = moduleSection.getString("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                int lastCollectedPlaytime = moduleSection.getInt("last-collected-playtime", 0);

                rewardUser.addModuleData(new PlaytimeDailyGoalsModuleUserData(Module.ModuleType.DAILY_PLAYTIME_GOALS.getName(), lastCollectedPlaytime, LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"))));
            }
        }

        if (ActivityRewarder.getModule(Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName()) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName());

            if (moduleSection != null) {
                int lastCollectedPlaytime = moduleSection.getInt("last-collected-playtime", 0);

                rewardUser.addModuleData(new PlaytimeGoalsModuleUserData(Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName(), lastCollectedPlaytime));
            }
        }

        return rewardUser;
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());

        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());

        if (rewardUser.getModuleData(Module.ModuleType.DAILY_REWARDS.getName()) instanceof DailyRewardsModuleUserData dailyRewardsModuleData) {
            String moduleName = Module.ModuleType.DAILY_REWARDS.getName();

            configurationSection.set(moduleName + ".day-num", dailyRewardsModuleData.getDayNum());
            configurationSection.set(moduleName + ".streak-length", dailyRewardsModuleData.getStreakLength());
            configurationSection.set(moduleName + ".highest-streak", dailyRewardsModuleData.getHighestStreak());
            configurationSection.set(moduleName + ".start-date", dailyRewardsModuleData.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            configurationSection.set(moduleName + ".last-collected-date", dailyRewardsModuleData.getLastCollectedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            configurationSection.set(moduleName + ".collected-dates", dailyRewardsModuleData.getCollectedDates());
        }

        if (rewardUser.getModuleData(Module.ModuleType.DAILY_PLAYTIME_GOALS.getName()) instanceof PlaytimeDailyGoalsModuleUserData dailyPlaytimeGoalsModuleUserData) {
            String moduleName = Module.ModuleType.DAILY_PLAYTIME_GOALS.getName();

            configurationSection.set(moduleName + ".last-collected-playtime", dailyPlaytimeGoalsModuleUserData.getLastCollectedPlaytime());
        }

        if (rewardUser.getModuleData(Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName()) instanceof PlaytimeGoalsModuleUserData globalPlaytimeGoalsModuleUserData) {
            String moduleName = Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName();

            configurationSection.set(moduleName + ".last-collected-playtime", globalPlaytimeGoalsModuleUserData.getLastCollectedPlaytime());
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
