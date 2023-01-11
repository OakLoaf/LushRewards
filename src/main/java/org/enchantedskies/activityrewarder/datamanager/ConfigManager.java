package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.enchantedskies.activityrewarder.rewardtypes.CmdReward;
import org.enchantedskies.activityrewarder.rewardtypes.ItemReward;
import org.enchantedskies.activityrewarder.rewardtypes.Reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private FileConfiguration config;
    private ArrayList<Reward> rewardsList;

    public ConfigManager() {
        config = plugin.getConfig();
        rewardsList = loadRewardList();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        plugin.saveConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        rewardsList = loadRewardList();
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public int getLoopLength() {
        return config.getInt("loop-length");
    }

    public ItemStack getSizeItem(String size) {
        return new ItemStack(Material.valueOf(config.getString("sizes." + size).toUpperCase()));
    }

    public ItemStack getCollectedItem() {
        return new ItemStack(Material.valueOf(config.getString("collected-item").toUpperCase()));
    }

    public Reward getReward(int day) {
        return rewardsList.get(day - 1);
    }

    public Reward getReward(ConfigurationSection rewardSection) {
        String rewardType = rewardSection.getString("type", "item");
        Reward reward = null;
        if (rewardType.equalsIgnoreCase("item")) reward = getItemReward(rewardSection);
        else if (rewardType.equalsIgnoreCase("command")) reward = getCmdReward(rewardSection);
        return reward;
    }

    public Reward getCustomReward(ConfigurationSection rewardSection, RewardUser rewardUser) {
        String rewardType = rewardSection.getString("type", "item");
        Reward reward = null;
        if (rewardType.equalsIgnoreCase("item")) reward = getItemReward(rewardSection);
        else if (rewardType.equalsIgnoreCase("command")) reward = getCmdReward(rewardSection, rewardUser);
        return reward;
    }

    private Reward getItemReward(ConfigurationSection rewardSection) {
        String size = rewardSection.getString("size", "small").toLowerCase();
        Material material = Material.valueOf(rewardSection.getString("reward", "STONE").toUpperCase());
        int count = rewardSection.getInt("count", 1);
        return new ItemReward(material, count, size);
    }

    private Reward getCmdReward(ConfigurationSection rewardSection) {
        return getCmdReward(rewardSection, null);
    }

    private Reward getCmdReward(ConfigurationSection rewardSection, RewardUser rewardUser) {
        String size = rewardSection.getString("size", "small").toLowerCase();
        List<String> commands = rewardSection.getStringList("reward");
        StringBuilder command = new StringBuilder();
        for (String aCommand : commands) {
            command.append(aCommand).append("|");
        }
        double count = rewardSection.getDouble("count", -1);
        if (count != -1 && rewardUser != null) {
            int currPlayTime = (int) getTicksToHours(Bukkit.getPlayer(rewardUser.getUUID()).getStatistic(Statistic.PLAY_ONE_MINUTE));
            int hoursDiff = currPlayTime - rewardUser.getPlayTime();
            return new CmdReward(command.toString(), size, count, hoursDiff);
        }
        return new CmdReward(command.toString(), size);
    }

    public ArrayList<Reward> loadRewardList() {
        ArrayList<Reward> rewardsList = new ArrayList<>();
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        Reward defaultReward = getReward(rewardsSection.getConfigurationSection("default"));
        HashMap<Integer, Reward> dayToReward = new HashMap<>();
        for (String key : rewardsSection.getKeys(false)) {
            if (key.equalsIgnoreCase("default")) continue;
            Reward thisReward = getReward(rewardsSection.getConfigurationSection(key));
            dayToReward.put(Integer.parseInt(key), thisReward);
        }
        for (int i = 0; i < config.getInt("loop-length"); i++) {
            Reward reward = defaultReward;
            if (dayToReward.containsKey(i)) reward = dayToReward.get(i);
            rewardsList.add(reward);
        }
        return rewardsList;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
