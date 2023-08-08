package me.dave.activityrewarder.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.rewards.*;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.HourlyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RewardManager {
    private final File rewardsFile = initYML();
    private final Multimap<Integer, DailyRewardCollection> dayToRewards = HashMultimap.create();
    private final Multimap<SimpleDate, DailyRewardCollection> dateToRewards = HashMultimap.create();
    private final HashMap<String, HourlyRewardCollection> permissionToHourlyReward = new HashMap<>();
    private DailyRewardCollection defaultReward;

    public RewardManager() {
        // TODO: Do this better.. probably best not to be async?
        ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().runDelayed(this::reloadRewards, Duration.of(1000, ChronoUnit.MILLIS));
    }

    public void reloadRewards() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);

        // Clears rewards maps
        dayToRewards.clear();
        dateToRewards.clear();
        permissionToHourlyReward.clear();

        if (ActivityRewarder.getConfigManager().areDailyRewardsEnabled()) {
            ConfigurationSection rewardDaysSection = config.getConfigurationSection("daily-rewards");
            if (rewardDaysSection != null) {
                rewardDaysSection.getValues(false).forEach((key, value) -> {
                    if (value instanceof ConfigurationSection rewardSection) {
                        DailyRewardCollection dailyRewardCollection = loadDailyRewardCollection(rewardSection);
                        if (dailyRewardCollection != null) {
                            if (rewardSection.getName().equalsIgnoreCase("default")) defaultReward = dailyRewardCollection;
                            else dayToRewards.put(Integer.parseInt(rewardSection.getName().replaceAll("\\D", "")), dailyRewardCollection);
                        }
                    }
                });

                ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + (dayToRewards.size() + (defaultReward != null ? 1 : 0)) + " reward collections from '" + rewardDaysSection.getCurrentPath() + "'");
            }
            else {
                ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section");
            }
        }

        if (ActivityRewarder.getConfigManager().areHourlyRewardsEnabled()) {
            ConfigurationSection hourlyRewardSection = config.getConfigurationSection("hourly-rewards");
            if (hourlyRewardSection != null) {
                hourlyRewardSection.getValues(false).forEach((key, value) -> {
                    if (value instanceof ConfigurationSection permissionSection) {
                        List<Map<?, ?>> rewardMaps = permissionSection.getMapList("rewards");
                        List<Reward> rewardList = !rewardMaps.isEmpty() ? loadRewards(rewardMaps, permissionSection.getCurrentPath() + "rewards") : new ArrayList<>();
                        if (rewardList != null) permissionToHourlyReward.put(key, new HourlyRewardCollection(permissionSection.getDouble("multiplier", 1), rewardList));
                    }
                });
            }
            else {
                ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'hourly-rewards' section");
            }
        }
    }

    public DailyRewardCollection getDefaultReward() {
        return defaultReward;
    }

    @NotNull
    public RewardDay getRewards(int day) {
        if (dayToRewards.containsKey(day)) return RewardDay.from(dayToRewards.get(day));
        else return RewardDay.from(defaultReward);
    }

    @Nullable
    public HourlyRewardCollection getHourlyRewards(Player player) {
        Debugger.sendDebugMessage("Getting hourly bonus section from config", Debugger.DebugMode.HOURLY);
        if (permissionToHourlyReward.isEmpty()) {
            Debugger.sendDebugMessage("No hourly rewards found", Debugger.DebugMode.HOURLY);
            return null;
        }

        Debugger.sendDebugMessage("Checking player's highest multiplier", Debugger.DebugMode.HOURLY);
        HourlyRewardCollection hourlyRewardCollection = getHighestMultiplierReward(player);
        if (hourlyRewardCollection != null) {
            Debugger.sendDebugMessage("Found highest multiplier (" + hourlyRewardCollection.getMultiplier() + ")", Debugger.DebugMode.HOURLY);
            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player.getUniqueId());
            rewardUser.setHourlyMultiplier(hourlyRewardCollection.getMultiplier());
        }
        else Debugger.sendDebugMessage("Could not find a valid multiplier for this player", Debugger.DebugMode.HOURLY);

        return hourlyRewardCollection;
    }

    @Nullable
    public HourlyRewardCollection getHighestMultiplierReward(Player player) {
        HourlyRewardCollection highestMultiplierReward = null;
        double highestMultiplier = 0;

        for (Map.Entry<String, HourlyRewardCollection> entry : permissionToHourlyReward.entrySet()) {
            String permission = entry.getKey();

            if (!player.hasPermission("activityrewarder.bonus." + permission)) continue;
            Debugger.sendDebugMessage("Player has activityrewarder.bonus." + permission, Debugger.DebugMode.HOURLY);

            double multiplier = entry.getValue().getMultiplier();
            if (multiplier > highestMultiplier) {
                Debugger.sendDebugMessage("Found higher multiplier, updated highest multiplier", Debugger.DebugMode.HOURLY);
                highestMultiplier = multiplier;
                highestMultiplierReward = entry.getValue();
            }
        }
        return highestMultiplierReward;
    }

    public double getHighestMultiplier(Player player) {
        double highestMultiplier = 0;

        for (Map.Entry<String, HourlyRewardCollection> entry : permissionToHourlyReward.entrySet()) {
            String permission = entry.getKey();

            if (!player.hasPermission("activityrewarder.bonus." + permission)) continue;
            Debugger.sendDebugMessage("Player has activityrewarder.bonus." + permission, Debugger.DebugMode.HOURLY);

            double multiplier = entry.getValue().getMultiplier();
            if (multiplier > highestMultiplier) {
                Debugger.sendDebugMessage("Found higher multiplier, updated highest multiplier", Debugger.DebugMode.HOURLY);
                highestMultiplier = multiplier;
            }
        }

        return highestMultiplier;
    }

    // TODO: Run once per day in performance mode
    public int findNextRewardFromCategory(int day, String category) {
        int nextRewardKey = -1;

        // Iterates through dayToRewards
        for (int rewardsKey : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardsKey <= day || (nextRewardKey != -1 && rewardsKey > nextRewardKey)) continue;

            // Gets the category of the reward and compares to the request
            RewardDay rewardDay = getRewards(rewardsKey);
            if (rewardDay == null) continue;
            if (rewardDay.containsRewardFromCategory(category)) nextRewardKey = rewardsKey;
        }

        // Returns -1 if no future rewards match the request
        return nextRewardKey;
    }

    @Nullable
    public Reward loadReward(Map<?, ?> rewardMap, String path) {
        String rewardType = (String) rewardMap.get("type");
        if (!RewardTypes.isRewardRegistered(rewardType)) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid reward type at '" + path + "'");
            return null;
        }

        try {
            return RewardTypes.getClass(rewardType).getConstructor(Map.class).newInstance(rewardMap);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public List<Reward> loadRewards(List<Map<?, ?>> maps, String path) {
        List<Reward> rewardList = new ArrayList<>();

        maps.forEach((map) -> {
            Reward reward = loadReward(map, path);
            if (reward != null) rewardList.add(reward);
        });

        return !rewardList.isEmpty() ? rewardList : null;
    }

    @Nullable
    private DailyRewardCollection loadDailyRewardCollection(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        String category = rewardCollectionSection.getString("category", "SMALL").toUpperCase();
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        List<String> lore = rewardCollectionSection.getStringList("lore");
        Debugger.sendDebugMessage("Reward collection lore set to:", debugMode);
        lore.forEach(str -> Debugger.sendDebugMessage("- " + str, debugMode));

        Sound redeemSound = ConfigParser.getSound(rewardCollectionSection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + "rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return rewardList != null ? DailyRewardCollection.from(rewardList, 0, category, lore, redeemSound) : DailyRewardCollection.empty();
    }

    private File initYML() {
        ActivityRewarder plugin = ActivityRewarder.getInstance();
        File rewardsFile = new File(plugin.getDataFolder(),"rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
            plugin.getLogger().info("File Created: rewards.yml");
        }
        return rewardsFile;
    }
}
