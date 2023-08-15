package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ConfigManager {
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private final HashMap<String, SimpleItemStack> categoryItems = new HashMap<>();
    private final HashMap<String, SimpleItemStack> itemTemplates = new HashMap<>();
    private final HashMap<String, String> messages = new HashMap<>();
    private File rewardsFile;
    private File playtimeRewardsFile;
    private boolean allowRewardsStacking;
    private boolean rewardsRefresh;
    private int reminderPeriod;
    private boolean streakMode;
    private String upcomingCategory;

    public ConfigManager() {
        ActivityRewarder.getInstance().saveDefaultConfig();
        initRewardsYmls();

        reloadConfig();
    }

    public void reloadConfig() {
        ActivityRewarder.getInstance().reloadConfig();
        FileConfiguration config = ActivityRewarder.getInstance().getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));

        allowRewardsStacking = config.getBoolean("allow-rewards-stacking", true);
        rewardsRefresh = config.getBoolean("rewards-refresh-daily", false);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        streakMode = config.getBoolean("streak-mode", false);
        upcomingCategory = config.getString("upcoming-category");

        if (config.getBoolean("modules.daily-rewards", false)) {
            ActivityRewarder.registerModule(new DailyRewardsModule("daily-rewards"));
        }
        if (config.getBoolean("modules.playtime-daily-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeDailyGoalsModule("playtime-daily-goals"));
        }
        if (config.getBoolean("modules.playtime-global-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeGlobalGoalsModule("playtime-global-goals"));
        }

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadMessages(config.getConfigurationSection("messages"));
        notificationHandler.reloadNotifications(reminderPeriod);

        if (ActivityRewarder.getModule("daily-rewards") instanceof DailyRewardsModule dailyRewardsModule) {
            dailyRewardsModule.reload();
        }
    }

    public YamlConfiguration getDailyRewardsConfig() {
        return YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public YamlConfiguration getPlaytimeRewardsConfig() {
        return YamlConfiguration.loadConfiguration(playtimeRewardsFile);
    }

    public String getMessage(String messageName) {
        return messages.getOrDefault(messageName, "");
    }

    public SimpleItemStack getCategoryTemplate(String category) {
        SimpleItemStack itemTemplate = categoryItems.get(category.toLowerCase());
        if (itemTemplate == null) {
            ActivityRewarder.getInstance().getLogger().severe("Could not find category '" + category + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public String getUpcomingCategory() {
        return upcomingCategory;
    }

    public SimpleItemStack getItemTemplate(String key) {
        SimpleItemStack itemTemplate = itemTemplates.get(key);
        if (itemTemplate == null) {
            ActivityRewarder.getInstance().getLogger().severe("Could not find item-template '" + key + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public boolean shouldStackRewards() {
        return allowRewardsStacking;
    }

    public boolean doRewardsRefresh() {
        return rewardsRefresh;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public boolean isStreakModeEnabled() {
        return streakMode;
    }

    private void reloadCategoryMap(ConfigurationSection categoriesSection) {
        // Clears category map
        categoryItems.clear();

        // Checks if categories section exists
        if (categoriesSection == null) {
            return;
        }

        // Repopulates category map
        categoriesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                categoryItems.put(categorySection.getName(), SimpleItemStack.from(categorySection));
            }
        });
    }

    private void reloadItemTemplates(ConfigurationSection itemTemplatesSection) {
        // Clears category map
        itemTemplates.clear();

        // Checks if categories section exists
        if (itemTemplatesSection == null) {
            return;
        }

        // Repopulates category map
        itemTemplatesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                itemTemplates.put(categorySection.getName(), SimpleItemStack.from(categorySection));
                ActivityRewarder.getInstance().getLogger().info("Loaded item-template: " + categorySection.getName());
            }
        });
    }

    private void reloadMessages(ConfigurationSection messagesSection) {
        // Clears messages map
        messages.clear();

        // Checks if messages section exists
        if (messagesSection == null) {
            return;
        }

        // Repopulates messages map
        messagesSection.getValues(false).forEach((key, value) -> messages.put(key, (String) value));
    }

    private void initRewardsYmls() {
        ActivityRewarder plugin = ActivityRewarder.getInstance();

        File dailyRewardsFile = new File(plugin.getDataFolder(), "daily-rewards.yml");
        if (!dailyRewardsFile.exists()) {
            plugin.saveResource("daily-rewards.yml", false);
            plugin.getLogger().info("File Created: daily-rewards.yml");
        }

        File playtimeRewardsFile = new File(plugin.getDataFolder(), "playtime-rewards.yml");
        if (!playtimeRewardsFile.exists()) {
            plugin.saveResource("playtime-rewards.yml", false);
            plugin.getLogger().info("File Created: playtime-rewards.yml");
        }

        this.rewardsFile = dailyRewardsFile;
        this.playtimeRewardsFile = playtimeRewardsFile;
    }
}
