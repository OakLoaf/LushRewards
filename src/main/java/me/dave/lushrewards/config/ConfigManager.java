package me.dave.lushrewards.config;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.lushrewards.utils.Debugger;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.manager.GuiManager;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.utils.SimpleItemStack;
import me.dave.platyutils.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ConfigManager {
    private static final File modulesFolder = new File(LushRewards.getInstance().getDataFolder(), "modules");
    private static LocalDate currentDate;

    private final ConcurrentHashMap<String, SimpleItemStack> categoryItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SimpleItemStack> globalItemTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> messages = new ConcurrentHashMap<>();
    private boolean performanceMode;

    private boolean playtimeIgnoreAfk;
    private int reminderPeriod;
    private Sound reminderSound;

    public ConfigManager() {
        if (!new File(PlatyUtils.getPlugin().getDataFolder(), "config.yml").exists()) {
            LushRewards.getInstance().saveDefaultResource("modules/daily-rewards.yml");
            LushRewards.getInstance().saveDefaultResource("modules/daily-playtime-goals.yml");
            LushRewards.getInstance().saveDefaultResource("modules/global-playtime-goals.yml");
        }

        LushRewards.getInstance().saveDefaultConfig();
    }

    public void reloadConfig() {
        LushRewards plugin = LushRewards.getInstance();
        PlatyUtils.getManager(GuiManager.class).ifPresent(GuiManager::closeAll);

        plugin.unregisterAllModules();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));
        performanceMode = config.getBoolean("enable-performance-mode", false);
        if (performanceMode) {
            currentDate = LocalDate.now();
        }

        playtimeIgnoreAfk = config.getBoolean("playtime-ignore-afk", true);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        reminderSound = StringUtils.getEnum(config.getString("reminder-sound", "none"), Sound.class).orElse(null);

        try {
            Files.newDirectoryStream(modulesFolder.toPath(), "*.yml").forEach(entry -> {
                File moduleFile = entry.toFile();
                YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(moduleFile);
                if (moduleConfig.getBoolean("enabled", true)) {
                    String moduleId = moduleConfig.getString("id");
                    if (moduleId == null) {
                        plugin.log(Level.SEVERE, "No module id was found in '" + moduleFile.getName() + "'");
                        return;
                    } else if (plugin.getModule(moduleId).isPresent()) {
                        plugin.log(Level.SEVERE, "A module with the id '" + moduleId + "' is already registered");
                        return;
                    }

                    String rewardsType = moduleConfig.getString("rewards-type");
                    if (rewardsType == null || !plugin.hasModuleType(rewardsType)) {
                        plugin.log(Level.SEVERE, "Module with id '" + moduleId + "' failed to register due to invalid value at 'rewards-type'");
                        return;
                    }

                    plugin.registerModule(plugin.loadModuleType(rewardsType, moduleId, moduleFile));
                }
            });
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Something went wrong whilst reading modules files");
            e.printStackTrace();
        }

        if (plugin.getModules().stream().anyMatch(module -> module instanceof RewardModule rewardModule && rewardModule.requiresPlaytimeTracker())) {
            plugin.registerModule(new PlaytimeTrackerModule());
        }

        boolean enableUpdater = config.getBoolean("enable-updater", true);
        plugin.getUpdater().setEnabled(enableUpdater);
        if (enableUpdater) {
            plugin.getUpdater().queueCheck();
        }

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadMessages(config.getConfigurationSection("messages"));
        plugin.getNotificationHandler().reloadNotifications();

        if (plugin.getDataManager() != null) {
            plugin.getDataManager().reloadRewardUsers();
        }

        plugin.getModules().forEach(Module::reload);
    }

    public String getMessage(String messageName) {
        return getMessage(messageName, "");
    }

    public String getMessage(String messageName, String def) {
        String output = messages.getOrDefault(messageName, def);

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
            LushRewards.getInstance().getLogger().severe("Could not find category '" + category + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public SimpleItemStack getItemTemplate(String key) {
        SimpleItemStack itemTemplate = globalItemTemplates.get(key);
        if (itemTemplate == null) {
            LushRewards.getInstance().getLogger().severe("Could not find item-template '" + key + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public boolean isPerformanceModeEnabled() {
        return performanceMode;
    }

    public void checkRefresh() {
        if (performanceMode && !currentDate.isEqual(LocalDate.now())) {
            reloadConfig();
        }
    }

    public boolean getPlaytimeIgnoreAfk() {
        return playtimeIgnoreAfk;
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
        globalItemTemplates.clear();

        // Checks if categories section exists
        if (itemTemplatesSection == null) {
            return;
        }

        // Repopulates category map
        itemTemplatesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                globalItemTemplates.put(categorySection.getName(), SimpleItemStack.from(categorySection));
                LushRewards.getInstance().getLogger().info("Loaded item-template: " + categorySection.getName());
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
}
