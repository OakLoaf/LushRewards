package me.dave.activityrewarder.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.rewards.*;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.PlaytimeRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class RewardManager {
    private final File rewardsFile = initYML();
    private final Multimap<Integer, DailyRewardCollection> dayToRewards = HashMultimap.create();
    private final Multimap<SimpleDate, DailyRewardCollection> dateToRewards = HashMultimap.create();
    private final HashMap<String, PlaytimeRewardCollection> permissionToPlaytimeReward = new HashMap<>();
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
        permissionToPlaytimeReward.clear();

        if (ActivityRewarder.getConfigManager().areDailyRewardsEnabled()) {
            ConfigurationSection rewardDaysSection = config.getConfigurationSection("daily-rewards");
            if (rewardDaysSection != null) {
                rewardDaysSection.getValues(false).forEach((key, value) -> {
                    if (value instanceof ConfigurationSection rewardSection) {
                        DailyRewardCollection dailyRewardCollection = loadDailyRewardCollection(rewardSection);
                        if (rewardSection.getName().equalsIgnoreCase("default")) defaultReward = dailyRewardCollection;
                        else dayToRewards.put(Integer.parseInt(rewardSection.getName().replaceAll("\\D", "")), dailyRewardCollection);
                    }
                });

                ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + (dayToRewards.size() + (defaultReward != null ? 1 : 0)) + " reward collections from '" + rewardDaysSection.getCurrentPath() + "'");
            }
            else {
                ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section");
            }
        }

        if (ActivityRewarder.getConfigManager().arePlaytimeRewardsEnabled()) {
            ConfigurationSection playtimeRewardsSection = config.getConfigurationSection("playtime-rewards");
            if (playtimeRewardsSection != null) {
                playtimeRewardsSection.getValues(false).forEach((key, value) -> {
                    if (value instanceof ConfigurationSection permissionSection) {
                        List<Map<?, ?>> rewardMaps = permissionSection.getMapList("rewards");
                        List<Reward> rewardList = !rewardMaps.isEmpty() ? loadRewards(rewardMaps, permissionSection.getCurrentPath() + "rewards") : new ArrayList<>();

                        if (rewardList != null) {
                            permissionToPlaytimeReward.put(key, new PlaytimeRewardCollection(permissionSection.getDouble("multiplier", 1), rewardList));
                        }
                    }
                });
            }
            else {
                ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'playtime-rewards' section");
            }
        }
    }

    public DailyRewardCollection getDefaultReward() {
        return defaultReward;
    }

    @NotNull
    public RewardDay getRewards(int day) {
        if (dayToRewards.containsKey(day)) {
            return RewardDay.from(dayToRewards.get(day));
        } else {
            return RewardDay.from(defaultReward);
        }
    }

    @Nullable
    public PlaytimeRewardCollection getHourlyRewards(Player player) {
        Debugger.sendDebugMessage("Getting playtime rewards section from config", Debugger.DebugMode.PLAYTIME);
        if (permissionToPlaytimeReward.isEmpty()) {
            Debugger.sendDebugMessage("No playtime rewards found", Debugger.DebugMode.PLAYTIME);
            return null;
        }

        Debugger.sendDebugMessage("Checking player's highest multiplier", Debugger.DebugMode.PLAYTIME);
        PlaytimeRewardCollection playtimeRewardCollection = getHighestMultiplierReward(player);
        if (playtimeRewardCollection != null) {
            Debugger.sendDebugMessage("Found highest multiplier (" + playtimeRewardCollection.getMultiplier() + ")", Debugger.DebugMode.PLAYTIME);
            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            rewardUser.setHourlyMultiplier(playtimeRewardCollection.getMultiplier());
        } else {
            Debugger.sendDebugMessage("Could not find a valid multiplier for this player", Debugger.DebugMode.PLAYTIME);
        }

        return playtimeRewardCollection;
    }

    @Nullable
    public PlaytimeRewardCollection getHighestMultiplierReward(Player player) {
        PlaytimeRewardCollection highestMultiplierReward = null;
        double highestMultiplier = 0;

        for (Map.Entry<String, PlaytimeRewardCollection> entry : permissionToPlaytimeReward.entrySet()) {
            String permission = entry.getKey();

            if (!player.hasPermission("activityrewarder.bonus." + permission)) {
                continue;
            }
            Debugger.sendDebugMessage("Player has activityrewarder.bonus." + permission, Debugger.DebugMode.PLAYTIME);

            double multiplier = entry.getValue().getMultiplier();
            if (multiplier > highestMultiplier) {
                Debugger.sendDebugMessage("Found higher multiplier, updated highest multiplier", Debugger.DebugMode.PLAYTIME);
                highestMultiplier = multiplier;
                highestMultiplierReward = entry.getValue();
            }
        }
        return highestMultiplierReward;
    }

    public double getHighestMultiplier(Player player) {
        double highestMultiplier = 0;

        for (Map.Entry<String, PlaytimeRewardCollection> entry : permissionToPlaytimeReward.entrySet()) {
            String permission = entry.getKey();

            if (!player.hasPermission("activityrewarder.bonus." + permission)) {
                continue;
            }
            Debugger.sendDebugMessage("Player has activityrewarder.bonus." + permission, Debugger.DebugMode.PLAYTIME);

            double multiplier = entry.getValue().getMultiplier();
            if (multiplier > highestMultiplier) {
                Debugger.sendDebugMessage("Found higher multiplier, updated highest multiplier", Debugger.DebugMode.PLAYTIME);
                highestMultiplier = multiplier;
            }
        }

        return highestMultiplier;
    }

    public int findNextRewardFromCategory(int day, String category) {
        int nextRewardDay = -1;

        // Iterates through dayToRewards
        for (int rewardDayNum : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardDayNum <= day || (nextRewardDay != -1 && rewardDayNum > nextRewardDay)) {
                continue;
            }

            // Gets the category of the reward and compares to the request
            RewardDay rewardDay = getRewards(rewardDayNum);
            if (rewardDay.containsRewardFromCategory(category)) {
                nextRewardDay = rewardDayNum;
            }
        }

        // Returns -1 if no future rewards match the request
        return nextRewardDay;
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
            if (reward != null) {
                rewardList.add(reward);
            }
        });

        return !rewardList.isEmpty() ? rewardList : null;
    }

    @NotNull
    private DailyRewardCollection loadDailyRewardCollection(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        String category = rewardCollectionSection.getString("category", "small");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        SimpleItemStack itemStack = itemSection != null ? SimpleItemStack.from(itemSection) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = ConfigParser.getSound(rewardCollectionSection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + "rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return rewardList != null ? DailyRewardCollection.from(rewardList, 0, category, itemStack, redeemSound) : DailyRewardCollection.empty();
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
