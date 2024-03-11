package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.RewardModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;

public class YmlStorage implements Storage<RewardUser, UUID> {
    private final File dataFolder = new File(ActivityRewarder.getInstance().getDataFolder(), "data");

    @Override
    public RewardUser load(UUID uuid) {
        ConfigurationSection configurationSection = loadOrCreateFile(uuid);

        String name = configurationSection.getString("name");
        int minutesPlayed = configurationSection.getInt("minutes-played", 0);

        RewardUser rewardUser = new RewardUser(uuid, name, minutesPlayed);

        ActivityRewarder.getInstance().getRewardModules().forEach(module -> {
            RewardModule.UserData userData = configurationSection.getObject(module.getId(), module.getUserDataClass());

            ConfigurationSection moduleSection = configurationSection.getConfigurationSection(module.getId());
            if (moduleSection == null) {
                configurationSection.createSection(module.getId());
                userData = module.getUserDataConstructor().build();
            }

            module.loadUserData(uuid, userData);
        });

        return rewardUser;
    }

    @Override
    public void save(RewardUser rewardUser) {
        YamlConfiguration configurationSection = loadOrCreateFile(rewardUser.getUniqueId());

        configurationSection.set("name", rewardUser.getUsername());
        configurationSection.set("minutes-played", rewardUser.getMinutesPlayed());

        if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData dailyRewardsModuleData) {
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

        if (rewardUser.getModuleData(PlaytimeDailyGoalsModule.ID) instanceof PlaytimeDailyGoalsModule.UserData dailyPlaytimeGoalsModuleUserData) {
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
