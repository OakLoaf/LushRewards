package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModuleUserData;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YmlStorage implements Storage<RewardUser, UUID> {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final File dataFolder = new File(plugin.getDataFolder(), "data");

    @Override
    public RewardUser load(UUID uuid) {
        ConfigurationSection configurationSection = loadOrCreateFile(uuid);

        String name = configurationSection.getString("name");
        int minutesPlayed = configurationSection.getInt("minutes-played", 0);

        RewardUser rewardUser = new RewardUser(uuid, name, minutesPlayed);

        if (ActivityRewarder.getModule(DailyRewardsModule.ID) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(DailyRewardsModule.ID);
            if (moduleSection == null) {
                moduleSection = configurationSection.createSection(DailyRewardsModule.ID);
            }

            int streakLength = moduleSection.getInt("streak-length", 0);
            int highestStreak = moduleSection.getInt("highest-streak", 0);
            HashSet<String> collectedDates =  new HashSet<>(moduleSection.getStringList("collected-dates"));

            String startDateRaw = moduleSection.getString("start-date");
            LocalDate startDate = startDateRaw != null ? LocalDate.parse(startDateRaw, DateTimeFormatter.ofPattern("dd-MM-yyyy")) : LocalDate.now();

            String lastCollectedDateRaw = moduleSection.getString("last-collected-date");
            LocalDate lastCollectedDate = lastCollectedDateRaw != null ? LocalDate.parse(lastCollectedDateRaw, DateTimeFormatter.ofPattern("dd-MM-yyyy")) : null;

            rewardUser.addModuleData(new DailyRewardsModuleUserData(DailyRewardsModule.ID, streakLength, highestStreak, startDate, lastCollectedDate, collectedDates));
        }

        if (ActivityRewarder.getModule(PlaytimeDailyGoalsModule.ID) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(PlaytimeDailyGoalsModule.ID);
            if (moduleSection == null) {
                moduleSection = configurationSection.createSection(PlaytimeDailyGoalsModule.ID);
            }

            int previousDayEnd = moduleSection.getInt("previous-day-end", 0);
            String date = moduleSection.getString("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            int lastCollectedPlaytime = moduleSection.getInt("last-collected-playtime", 0);

            rewardUser.addModuleData(new PlaytimeDailyGoalsModuleUserData(PlaytimeDailyGoalsModule.ID, lastCollectedPlaytime, LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy")), previousDayEnd));
        }

        if (ActivityRewarder.getModule(PlaytimeGlobalGoalsModule.ID) != null) {
            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(PlaytimeGlobalGoalsModule.ID);
            if (moduleSection == null) {
                moduleSection = configurationSection.createSection(PlaytimeGlobalGoalsModule.ID);
            }

            int lastCollectedPlaytime = moduleSection.getInt("last-collected-playtime", 0);

            rewardUser.addModuleData(new PlaytimeGoalsModuleUserData(PlaytimeGlobalGoalsModule.ID, lastCollectedPlaytime));
        }

        return rewardUser;
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());

        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());

        if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModuleUserData dailyRewardsModuleData) {
            String moduleName = DailyRewardsModule.ID;

            configurationSection.set(moduleName + ".day-num", dailyRewardsModuleData.getDayNum());
            configurationSection.set(moduleName + ".streak-length", dailyRewardsModuleData.getStreakLength());
            configurationSection.set(moduleName + ".highest-streak", dailyRewardsModuleData.getHighestStreak());
            configurationSection.set(moduleName + ".start-date", dailyRewardsModuleData.getStartDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            if (dailyRewardsModuleData.getLastCollectedDate() != null) {
                configurationSection.set(moduleName + ".last-collected-date", dailyRewardsModuleData.getLastCollectedDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            } else {
                configurationSection.set(moduleName + ".last-collected-date", null);
            }
            configurationSection.set(moduleName + ".collected-dates", new ArrayList<>(dailyRewardsModuleData.getCollectedDates()));
        }

        if (rewardUser.getModuleData(PlaytimeDailyGoalsModule.ID) instanceof PlaytimeDailyGoalsModuleUserData dailyPlaytimeGoalsModuleUserData) {
            String moduleName = PlaytimeDailyGoalsModule.ID;

            configurationSection.set(moduleName + ".date", dailyPlaytimeGoalsModuleUserData.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            configurationSection.set(moduleName + ".last-collected-playtime", dailyPlaytimeGoalsModuleUserData.getLastCollectedPlaytime());
            configurationSection.set(moduleName + ".previous-day-end", dailyPlaytimeGoalsModuleUserData.getPreviousDayEndPlaytime());
        }

        if (rewardUser.getModuleData(PlaytimeGlobalGoalsModule.ID) instanceof PlaytimeGoalsModuleUserData globalPlaytimeGoalsModuleUserData) {
            String moduleName = PlaytimeGlobalGoalsModule.ID;

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
