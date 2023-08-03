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
import me.dave.activityrewarder.rewards.custom.CommandReward;
import me.dave.activityrewarder.rewards.custom.ItemReward;
import me.dave.activityrewarder.rewards.Reward;

import javax.annotation.Nullable;
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
        return categoryItems.get(category.toLowerCase()).clone();
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
        for (String perm : hourlySection.getKeys(false)) {
            if (perm.equals("enabled")) continue;
            sendDebugMessage("Checking if player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                sendDebugMessage("Player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
                double multiplier = hourlySection.getConfigurationSection(perm).getDouble("multiplier", 1);

                if (multiplier > heighestMultiplier) {
                    sendDebugMessage("Found higher multiplier, updated highest multiplier", DebugMode.HOURLY);
                    heighestMultiplier = multiplier;
                    hourlyRewards = loadSectionRewards(hourlySection.getConfigurationSection(perm), DebugMode.HOURLY);
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
            if (rewards.getCategory().equalsIgnoreCase(category)) nextRewardKey = rewardsKey;
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

        rewardDaysSection.getValues(false).entrySet().forEach((data) -> {
            if (data instanceof ConfigurationSection rewardSection) {
                if (rewardSection.getName().equalsIgnoreCase("default")) {
                    defaultReward= loadSectionRewards(rewardSection, DebugMode.DAILY);
                }
                else {
                    dayToRewards.put(Integer.parseInt(rewardSection.getName()), loadSectionRewards(rewardSection, DebugMode.DAILY));
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
    private Reward loadReward(ConfigurationSection configurationSection, DebugMode debugMode) {
        String rewardType = configurationSection.getString("type", "e");
        if (RewardTypes.isRewardRegistered(rewardType)) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid reward type at '" + configurationSection.getCurrentPath() + "'");
            return null;
        }
        else {
            return RewardTypes.getClass(rewardType);
        }
    }

    private RewardCollection loadSectionRewards(ConfigurationSection rewardDaySection, DebugMode debugMode) {
        sendDebugMessage("Attempting to load sections reward (" + rewardDaySection.getCurrentPath() + ")", debugMode);
        ArrayList<Reward> rewards = new ArrayList<>();
        String category = rewardDaySection.getString("category", "SMALL").toUpperCase();
        sendDebugMessage("Reward category set to " + category, debugMode);
        List<String> lore = new ArrayList<>();
        if (rewardDaySection.getKeys(false).contains("lore")) {
            lore = rewardDaySection.getStringList("lore");
        }
        sendDebugMessage("Lore set", debugMode);
        lore.forEach(str -> sendDebugMessage("- " + str, debugMode));

        sendDebugMessage("Attempting to load item rewards", debugMode);
        ConfigurationSection itemRewards = rewardDaySection.getConfigurationSection("rewards.items");
        int itemRewardCount = 0;
        if (itemRewards != null) {
            for (String materialName : itemRewards.getKeys(false)) {
                ItemStack item = ConfigParser.getItem(materialName.toUpperCase(), Material.GOLD_NUGGET);
                int amount = itemRewards.getInt(materialName + ".amount", 1);
                item.setAmount(amount);

                rewards.add(new ItemReward(item));
                itemRewardCount++;
            }
        }
        sendDebugMessage("Successfully loaded " + itemRewardCount + " item rewards", debugMode);

        sendDebugMessage("Attempting to load command rewards", debugMode);
        int cmdRewardCount = 0;
        for (String command : rewardDaySection.getStringList("rewards.commands")) {
            rewards.add(new CommandReward(command));
            cmdRewardCount++;
        }
        sendDebugMessage("Successfully loaded " + cmdRewardCount + " command rewards", debugMode);

        sendDebugMessage("Successfully loaded " + (itemRewardCount + cmdRewardCount) + " total rewards", debugMode);
        return new RewardCollection(0, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, category, lore, rewards);
    }

    public record UpcomingReward(boolean enabled, List<String> lore) { }
}
