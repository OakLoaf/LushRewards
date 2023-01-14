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
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private FileConfiguration config;
    private ArrayList<Reward> defaultReward;
    private ArrayList<Reward> rewardsList;
    private int loopLength;
    private ArrayList<Reward> hourlyBonus;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        loopLength = config.getInt("loop-length");
        hourlyBonus =

        rewardsList = getRewardList();
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "§8§lDaily Rewards");
    }

    public String getGuiItemRedeemableName(int day) {
        return config.getString("gui.redeemable-name", "&6Day %day%").replaceAll("%day%", String.valueOf(day));
    }

    public String getGuiItemCollectedName(int day) {
        return config.getString("gui.collected-name", "&6Day %day% - Collected").replaceAll("%day%", String.valueOf(day));
    }

    public ItemStack getSizeItem(String size) {
        return new ItemStack(Material.valueOf(config.getString("sizes." + size).toUpperCase()));
    }

    public ItemStack getCollectedItem() {
        return new ItemStack(Material.valueOf(config.getString("collected-item").toUpperCase()));
    }

    public int getLoopLength() {
        return loopLength;
    }

    public Reward getReward(int day) {
        return rewardsList.get(day);
    }

    private Reward getReward(ConfigurationSection rewardSection) {
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
        Material material = Material.valueOf(rewardSection.getString("reward", "STONE").toUpperCase());
        int count = rewardSection.getInt("count", 1);
        ItemStack item = new ItemStack(material);
        item.setAmount(count);

        return new ItemReward(item);
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
            return new CmdReward(command.toString());
        }
        return new CmdReward(command.toString());
    }

    public ArrayList<Reward> loadRewardList() {
        ArrayList<Reward> rewardsList = new ArrayList<>();
        ConfigurationSection rewardsSection = config.getConfigurationSection("reward-days");
        Reward defaultReward = getReward(rewardsSection.getConfigurationSection("default"));
        HashMap<Integer, Reward> dayToReward = new HashMap<>();
        for (String key : rewardsSection.getKeys(false)) {
            Reward thisReward = getReward(rewardsSection.getConfigurationSection(key));
            dayToReward.put(Integer.parseInt(key) + 1, thisReward);
        }
        for (int day = 1; day < config.getInt("loop-length") + 1; day++) {
            Reward reward = defaultReward;
            if (dayToReward.containsKey(day)) reward = dayToReward.get(day);
            rewardsList.add(reward);
        }
        return rewardsList;
    }

    private ArrayList<Reward> getRewardList() {
        ConfigurationSection rewardsSection = config.getConfigurationSection("reward-days");

        HashMap<Integer, Reward> dayToReward = new HashMap<>();

        int loopLength = config.getInt("loop-length", -1);

        Set<String> rewardsKeys = rewardsSection.getKeys(false);
        for (String rewardsKey : rewardsKeys) {
            if (rewardsKey.equalsIgnoreCase("default")) {
                defaultReward = loadSectionRewards(rewardsSection.getConfigurationSection(rewardsKey));
            }
            Integer.parseInt(rewardsKey);
        }


        ArrayList<Reward> rewardsList = new ArrayList<>();



        return rewardsList;
    }

    private ArrayList<Reward> loadSectionRewards(ConfigurationSection rewardsSection) {
        ArrayList<Reward> rewards = new ArrayList<>();

        ConfigurationSection itemRewards = rewardsSection.getConfigurationSection("items");
        if (itemRewards != null) {
            for (String materialName : itemRewards.getKeys(false)) {
                ItemStack item = new ItemStack(Material.valueOf(materialName));
                int amount = itemRewards.getInt(materialName + ".amount", 1);
                item.setAmount(amount);

                rewards.add(new ItemReward(item));
            }
        }

        for (String command : rewardsSection.getStringList("commands")) {
            rewards.add(new CmdReward(command));
        }

        return rewards;
    }

    private long getTicksToHours(long ticksPlayed) {
        return TimeUnit.HOURS.convert(ticksPlayed * 50, TimeUnit.MILLISECONDS);
    }
}
