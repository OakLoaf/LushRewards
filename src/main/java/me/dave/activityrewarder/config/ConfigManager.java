package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.InventoryHandler;
import me.dave.activityrewarder.importer.ActivityRewarderConfigUpdater;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private final ConcurrentHashMap<String, SimpleItemStack> categoryItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SimpleItemStack> itemTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> messages = new ConcurrentHashMap<>();
    private File rewardsFile;
    private File dailyGoalsFile;
    private File globalGoalsFile;
    private boolean performanceMode;
    private LocalDate date;
    private int reminderPeriod;
    private Sound reminderSound;

    public ConfigManager() {
        if (isOutdated()) {
            try {
                new ActivityRewarderConfigUpdater().startImport().thenAccept(success -> {
                    reloadConfig();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ActivityRewarder.getInstance().saveDefaultConfig();
        initRewardsYmls();
    }

    public void reloadConfig() {
        InventoryHandler.closeAll();

        ActivityRewarder.getInstance().reloadConfig();
        FileConfiguration config = ActivityRewarder.getInstance().getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));
        performanceMode = config.getBoolean("enable-performance-mode", false);
        if (performanceMode) {
            date = LocalDate.now();
        }

        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        reminderSound = ConfigParser.getSound(config.getString("reminder-sound", "none").toUpperCase());

        ActivityRewarder.unregisterAllModules();

        boolean requiresPlaytimeTracker = false;
        if (config.getBoolean("modules.daily-rewards", false)) {
            ActivityRewarder.registerModule(new DailyRewardsModule(DailyRewardsModule.ID));
        }
        if (config.getBoolean("modules.daily-playtime-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeDailyGoalsModule(PlaytimeDailyGoalsModule.ID));
            requiresPlaytimeTracker = true;
        }
        if (config.getBoolean("modules.global-playtime-goals", false)) {
            ActivityRewarder.registerModule(new PlaytimeGlobalGoalsModule(PlaytimeGlobalGoalsModule.ID));
            requiresPlaytimeTracker = true;
        }
        if (requiresPlaytimeTracker) {
            PlaytimeTrackerModule module = new PlaytimeTrackerModule(PlaytimeTrackerModule.ID);
            ActivityRewarder.registerModule(module);

            if (ActivityRewarder.getDataManager() != null) {
                Bukkit.getOnlinePlayers().forEach(module::startPlaytimeTracker);
            }
        }

        boolean enableUpdater = config.getBoolean("enable-updater", true);
        ActivityRewarder.getInstance().getUpdater().setEnabled(enableUpdater);
        if (enableUpdater) {
            try {
                ActivityRewarder.getInstance().getUpdater().check();
            } catch (Exception e) {
                ActivityRewarder.getInstance().getLogger().severe("Unable to check for update: " + e.getMessage());
            }
        }

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadMessages(config.getConfigurationSection("messages"));
        ActivityRewarder.getNotificationHandler().reloadNotifications();

        if (ActivityRewarder.getDataManager() != null) {
            ActivityRewarder.getDataManager().reloadRewardUsers();
        }
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

    public SimpleItemStack getItemTemplate(String key) {
        SimpleItemStack itemTemplate = itemTemplates.get(key);
        if (itemTemplate == null) {
            ActivityRewarder.getInstance().getLogger().severe("Could not find item-template '" + key + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public boolean isPerformanceModeEnabled() {
        return performanceMode;
    }

    public void checkRefresh() {
        if (performanceMode && !date.isEqual(LocalDate.now())) {
            reloadConfig();
        }
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public Sound getReminderSound() {
        return reminderSound;
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

    private boolean isOutdated() {
        FileConfiguration config = ActivityRewarder.getInstance().getConfig();

        return config.contains("reward-days", true) && !config.contains("modules", true);
    }
}
