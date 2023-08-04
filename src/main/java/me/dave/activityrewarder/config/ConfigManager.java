package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.rewards.RewardCollection;
import me.dave.activityrewarder.rewards.RewardTypes;
import me.dave.activityrewarder.utils.ConfigParser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.dave.activityrewarder.rewards.Reward;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final Logger logger = plugin.getLogger();
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private FileConfiguration config;
    private DebugMode debugMode;
    private RewardCollection defaultReward;
    private final HashMap<Integer, RewardCollection> dayToRewards = new HashMap<>();
    private final HashMap<String, ItemStack> categoryItems = new HashMap<>();
    private GuiTemplate guiTemplate;
    private ItemStack collectedItem;
    private ItemStack borderItem;
    private UpcomingReward upcomingReward;
    private int loopLength;
    private int reminderPeriod;
    private boolean daysReset;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        debugMode = DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase());

        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        if (templateType.equals("CUSTOM")) guiTemplate = new GuiTemplate(config.getStringList("gui.format"));
        else guiTemplate = GuiTemplate.DefaultTemplate.valueOf(templateType);

        collectedItem = ConfigParser.getItem(config.getConfigurationSection("gui.collected-item"), Material.REDSTONE_BLOCK);
        borderItem = ConfigParser.getItem(config.getConfigurationSection("gui.border-item"), Material.GRAY_STAINED_GLASS_PANE);

        boolean showUpcomingReward = config.getBoolean("gui.upcoming-reward.enabled", true);
        List<String> upcomingRewardLore = config.getStringList("gui.upcoming-reward.lore");
        upcomingReward = new UpcomingReward(showUpcomingReward, upcomingRewardLore);

        loopLength = config.getInt("loop-length", -1);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        daysReset = config.getBoolean("days-reset", false);


        reloadRewardsMap();
        reloadCategoryMap();
        notificationHandler.reloadNotifications(reminderPeriod);
    }

    public void sendDebugMessage(String string, DebugMode mode) {
        if (debugMode == mode || debugMode == DebugMode.ALL) logger.info("DEBUG >> " + string);
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public String getRewardMessage() {
        return config.getString("messages.reward-given", "&e&lRewards &8» &aYou have collected today's reward");
    }

    public String getBonusMessage() {
        return config.getString("messages.hourly-bonus-given", "&e&lRewards &8» &7You have received a bonus for playing &e%hours% &7hours");
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "&8&lDaily Rewards");
    }

    public GuiTemplate getGuiTemplate() {
        return guiTemplate;
    }

    public boolean showUpcomingReward() {
        return upcomingReward.enabled;
    }

    public List<String> getUpcomingRewardLore() {
        return upcomingReward.lore;
    }

    public String getGuiItemRedeemableName(int day) {
        return config.getString("gui.redeemable-name", "&6Day %day%").replaceAll("%day%", String.valueOf(day));
    }

    public String getGuiItemCollectedName(int day) {
        return config.getString("gui.collected-name", "&6Day %day% - Collected").replaceAll("%day%", String.valueOf(day));
    }

    public ItemStack getCategoryItem(String category) {
        ItemStack item = categoryItems.get(category.toLowerCase());
        return item != null ? item.clone() : new ItemStack(Material.CHEST_MINECART);
    }

    public ItemStack getCollectedItem() {
        return collectedItem.clone();
    }

    public ItemStack getBorderItem() {
        return borderItem.clone();
    }

    public int getLoopLength() {
        return loopLength;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public boolean doDaysReset() {
        return daysReset;
    }

    public RewardCollection getDefaultReward() {
        return defaultReward;
    }

    public double getHourlyMultiplier(Player player) {
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        if (hourlySection == null || !hourlySection.getBoolean("enabled", false)) return 1;

        double heighestMultiplier = 1;
        for (String perm : hourlySection.getKeys(false)) {
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                double multiplier = hourlySection.getDouble(perm + ".multiplier", 1);
                if (multiplier > heighestMultiplier) heighestMultiplier = multiplier;
            }
        }

        return heighestMultiplier;
    }

    public RewardCollection getHourlyRewards(Player player) {
        sendDebugMessage("Getting hourly bonus section from config", DebugMode.HOURLY);
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        if (hourlySection == null) return null;
        sendDebugMessage("Checking if hourly bonus is enabled", DebugMode.HOURLY);
        if (!hourlySection.getBoolean("enabled", false)) return null;
        RewardCollection hourlyRewards = null;

        sendDebugMessage("Checking player's highest multiplier", DebugMode.HOURLY);
        double heighestMultiplier = 0;

        for (Map.Entry<String, Object> mappings : hourlySection.getValues(false).entrySet()) {
            String perm = mappings.getKey();
            if (perm.equals("enabled")) continue;

            sendDebugMessage("Checking if player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                if (mappings.getValue() instanceof ConfigurationSection valueSection) {
                    sendDebugMessage("Player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
                    double multiplier = valueSection.getDouble("multiplier", 1);

                    if (multiplier > heighestMultiplier) {
                        sendDebugMessage("Found higher multiplier, updated highest multiplier", DebugMode.HOURLY);
                        heighestMultiplier = multiplier;
                        hourlyRewards = loadRewardCollection(valueSection, DebugMode.HOURLY);
                    }
                }
            }
        }

        sendDebugMessage("Found highest multiplier (" + heighestMultiplier + ")", DebugMode.HOURLY);
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        rewardUser.setHourlyMultiplier(heighestMultiplier);

        return hourlyRewards;
    }

    public RewardCollection getRewards(int day) {
        // Works out what day number the user is in the loop
        int loopedDayNum = day;
        if (day > getLoopLength()) {
            loopedDayNum = (day % getLoopLength()) + 1;
        }

        if (dayToRewards.containsKey(day)) return dayToRewards.get(day);
        else if (dayToRewards.containsKey(loopedDayNum)) return dayToRewards.get(loopedDayNum);
        else return defaultReward;
    }

    public int findNextRewardInCategory(int day, String category) {
        int nextRewardKey = -1;

        // Iterates through dayToRewards
        for (int rewardsKey : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardsKey <= day || (nextRewardKey != -1 && rewardsKey > nextRewardKey)) continue;

            // Gets the category of the reward and compares to the request
            RewardCollection rewards = getRewards(rewardsKey);
            if (rewards.category().equalsIgnoreCase(category)) nextRewardKey = rewardsKey;
        }

        // Returns -1 if no future rewards match the request
        return nextRewardKey;
    }

    private void reloadRewardsMap() {
        // Clears rewards map
        dayToRewards.clear();

        ConfigurationSection rewardDaysSection = config.getConfigurationSection("reward-days");
        if (rewardDaysSection == null) rewardDaysSection = config.getConfigurationSection("rewards");
        if (rewardDaysSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'rewards' section");
            return;
        }

        rewardDaysSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection rewardSection) {
                if (rewardSection.getName().equalsIgnoreCase("default")) {
                    defaultReward = loadRewardCollection(rewardSection, DebugMode.DAILY);
                } else {
                    dayToRewards.put(Integer.parseInt(rewardSection.getName()), loadRewardCollection(rewardSection, DebugMode.DAILY));
                }
            }
        });
    }

    private void reloadCategoryMap() {
        // Clears category map
        categoryItems.clear();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        // Repopulates category map
        for (String category : categoriesSection.getKeys(false)) {
            categoryItems.put(category, ConfigParser.getItem(categoriesSection.getConfigurationSection(category), Material.STONE));
        }
    }

    @Nullable
    private Reward loadReward(ConfigurationSection configurationSection) {
        String rewardType = configurationSection.getString("type", "e");
        if (!RewardTypes.isRewardRegistered(rewardType)) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid reward type at '" + configurationSection.getCurrentPath() + "'");
            return null;
        }

        try {
            return RewardTypes.getClass(rewardType).getConstructor(ConfigurationSection.class).newInstance(configurationSection);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    private List<Reward> loadRewards(ConfigurationSection configurationSection) {
        List<Reward> rewardList = new ArrayList<>();

        configurationSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection rewardSection) {
                Reward reward = loadReward(rewardSection);
                if (reward != null) rewardList.add(reward);
            }
        });

        return rewardList;
    }

    private RewardCollection loadRewardCollection(ConfigurationSection rewardDaySection, DebugMode debugMode) {
        sendDebugMessage("Attempting to load reward collection at '" + rewardDaySection.getCurrentPath() + "'", debugMode);

        String category = rewardDaySection.getString("category", "SMALL").toUpperCase();
        sendDebugMessage("Reward category set to " + category, debugMode);

        List<String> lore = rewardDaySection.getStringList("lore");
        sendDebugMessage("Lore set to:", debugMode);
        lore.forEach(str -> sendDebugMessage("- " + str, debugMode));

        Sound redeemSound = ConfigParser.getSound(rewardDaySection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());

        sendDebugMessage("Attempting to load rewards", debugMode);

        ConfigurationSection rewardsSection = rewardDaySection.getConfigurationSection("rewards");

        Reward[] randomObject = rewardDaySection.getObject("rewards", Reward[].class);
        List<Reward> rewardList = rewardsSection != null ? loadRewards(rewardsSection) : new ArrayList<>();
        sendDebugMessage("Successfully loaded " + rewardList.size() + " rewards from '" + rewardDaySection.getCurrentPath() + "'", debugMode);

        return new RewardCollection(0, category, lore, redeemSound, rewardList);
    }

    public record UpcomingReward(boolean enabled, List<String> lore) { }
}
