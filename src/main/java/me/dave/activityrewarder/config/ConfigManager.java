package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimegoals.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimegoals.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private final ConcurrentHashMap<String, SimpleItemStack> categoryItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SimpleItemStack> itemTemplates = new ConcurrentHashMap<>();
    private final HashMap<String, String> messages = new HashMap<>();
    private File rewardsFile;
    private File dailyGoalsFile;
    private File globalGoalsFile;
    private boolean allowRewardsStacking;
    private boolean rewardsRefresh;
    private int reminderPeriod;
    private Sound reminderSound;
    private boolean streakMode;
    private String upcomingCategory;

    public ConfigManager() {
        ActivityRewarder.getInstance().saveDefaultConfig();
        initRewardsYmls();
    }

    public void reloadConfig() {
        ActivityRewarder.getInstance().reloadConfig();
        FileConfiguration config = ActivityRewarder.getInstance().getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));

        allowRewardsStacking = config.getBoolean("allow-rewards-stacking", true);
        rewardsRefresh = config.getBoolean("rewards-refresh-daily", false);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        reminderSound = ConfigParser.getSound(config.getString("reminder-sound", "none"));
        streakMode = config.getBoolean("streak-mode", false);
        upcomingCategory = config.getString("upcoming-category");

        ActivityRewarder.unregisterAll();

        boolean requiresPlaytimeTracker = false;
        if (config.getBoolean("modules.daily-rewards", false)) {
            ActivityRewarder.registerModule(new DailyRewardsModule("daily-rewards"));
        }
        if (config.getBoolean("modules.playtime-daily-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeDailyGoalsModule("playtime-daily-goals"));
            requiresPlaytimeTracker = true;
        }
        if (config.getBoolean("modules.playtime-global-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeGlobalGoalsModule("playtime-global-goals"));
            requiresPlaytimeTracker = true;
        }
        if (requiresPlaytimeTracker) {
            ActivityRewarder.registerModule(new PlaytimeTrackerModule("playtime-tracker"));
        }

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadMessages(config.getConfigurationSection("messages"));
        notificationHandler.reloadNotifications(reminderPeriod);
    }

    public YamlConfiguration getDailyRewardsConfig() {
        return YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public YamlConfiguration getDailyGoalsConfig() {
        return YamlConfiguration.loadConfiguration(dailyGoalsFile);
    }

    public YamlConfiguration getGlobalGoalsConfig() {
        return YamlConfiguration.loadConfiguration(globalGoalsFile);
    }

    public String getMessage(String messageName) {
        String output = messages.getOrDefault(messageName, "");

        if (messages.containsKey("prefix")) {
            return output.replaceAll("%prefix%", messages.get("prefix"));
        } else {
            return output;
        }
    }

    public Collection<String> getMessages() {
        return messages.values();
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

    public Sound getReminderSound() {
        return reminderSound;
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

        File dailyRewardsFile = new File(plugin.getDataFolder(), "modules/daily-rewards.yml");
        if (!dailyRewardsFile.exists()) {
            plugin.saveResource("modules/daily-rewards.yml", false);
            plugin.getLogger().info("File Created: daily-rewards.yml");
        }

        File dailyGoalsFile = new File(plugin.getDataFolder(), "modules/daily-playtime-goals.yml");
        if (!dailyGoalsFile.exists()) {
            plugin.saveResource("modules/daily-playtime-goals.yml", false);
            plugin.getLogger().info("File Created: daily-playtime-goals.yml");
        }

        File globalGoalsFile = new File(plugin.getDataFolder(), "modules/global-playtime-goals.yml");
        if (!globalGoalsFile.exists()) {
            plugin.saveResource("modules/global-playtime-goals.yml", false);
            plugin.getLogger().info("File Created: global-playtime-goals.yml");
        }

        this.rewardsFile = dailyRewardsFile;
        this.dailyGoalsFile = dailyGoalsFile;
        this.globalGoalsFile = globalGoalsFile;
    }
}
