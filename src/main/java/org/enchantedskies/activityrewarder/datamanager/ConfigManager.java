package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.enchantedskies.activityrewarder.rewards.CmdReward;
import org.enchantedskies.activityrewarder.rewards.ItemReward;
import org.enchantedskies.activityrewarder.rewards.Reward;
import org.enchantedskies.activityrewarder.rewards.RewardsDay;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private FileConfiguration config;
    private RewardsDay defaultReward;
    private final HashMap<Integer, RewardsDay> dayToRewards = new HashMap<>();
    private int loopLength;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loopLength = config.getInt("loop-length", -1);

        reloadRewardsMap();
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "&8&lDaily Rewards");
    }

    public String getGuiItemRedeemableName(int day) {
        return config.getString("gui.redeemable-name", "&6Day %day%").replaceAll("%day%", String.valueOf(day));
    }

    public String getGuiItemCollectedName(int day) {
        return config.getString("gui.collected-name", "&6Day %day% - Collected").replaceAll("%day%", String.valueOf(day));
    }

    public ItemStack getSizeItem(String size) {
        return new ItemStack(Material.valueOf(config.getString("sizes." + size.toLowerCase(), "STONE").toUpperCase()));
    }

    public ItemStack getCollectedItem() {
        return new ItemStack(Material.valueOf(config.getString("collected-item", "REDSTONE_BLOCK").toUpperCase()));
    }

    public int getLoopLength() {
        return loopLength;
    }

    public RewardsDay getHourlyRewards(Player player) {
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        RewardsDay hourlyRewards = null;

        double heighestMultiplier = -1;
        for (String perm : hourlySection.getKeys(false)) {
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                double multiplier = hourlySection.getConfigurationSection(perm).getDouble("multiplier", 1);

                if (multiplier > heighestMultiplier) {
                    heighestMultiplier = multiplier;
                    hourlyRewards = loadSectionRewards(hourlySection.getConfigurationSection(perm));
                    hourlyRewards.setMultiplier(multiplier);
                }
            }
        }

        return hourlyRewards;
    }

    public RewardsDay getRewards(int day) {
        // Works out what day number the user is in the loop
        int loopedDayNum = day % ActivityRewarder.configManager.getLoopLength();

        if (dayToRewards.containsKey(day)) return dayToRewards.get(day);
        else if (dayToRewards.containsKey(loopedDayNum)) return dayToRewards.get(loopedDayNum);
        else return defaultReward;
    }

    public int findNextRewardOfSize(int day, String size) {
        // Iterates through dayToRewards
        for (int rewardsKey : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardsKey <= day) continue;

            // Gets the size of the reward and compares to the request
            RewardsDay rewards = getRewards(rewardsKey);
            if (rewards.getSize().equalsIgnoreCase(size)) return rewardsKey;
        }

        // Returns -1 if no future rewards match the request
        return -1;
    }

    private void reloadRewardsMap() {
        // Clears rewards map
        dayToRewards.clear();

        ConfigurationSection rewardsSection = config.getConfigurationSection("reward-days");
        for (String rewardsKey : rewardsSection.getKeys(false)) {
            if (rewardsKey.equalsIgnoreCase("default")) {
                defaultReward = loadSectionRewards(rewardsSection.getConfigurationSection(rewardsKey));
                continue;
            }
            dayToRewards.put(Integer.parseInt(rewardsKey), loadSectionRewards(rewardsSection.getConfigurationSection(rewardsKey)));
        }
    }

    private RewardsDay loadSectionRewards(ConfigurationSection rewardsSection) {
        ArrayList<Reward> rewards = new ArrayList<>();
        String size = rewardsSection.getString("rewards.size", "SMALL").toUpperCase();

        ConfigurationSection itemRewards = rewardsSection.getConfigurationSection("rewards.items");
        if (itemRewards != null) {
            for (String materialName : itemRewards.getKeys(false)) {
                ItemStack item = new ItemStack(Material.valueOf(materialName.toUpperCase()));
                int amount = itemRewards.getInt(materialName + ".amount", 1);
                item.setAmount(amount);

                rewards.add(new ItemReward(item));
            }
        }

        for (String command : rewardsSection.getStringList("rewards.commands")) {
            rewards.add(new CmdReward(command));
        }

        return new RewardsDay(size, rewards);
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
