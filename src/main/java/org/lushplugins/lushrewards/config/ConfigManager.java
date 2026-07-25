package org.lushplugins.lushrewards.config;

import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lushplugins.lushlib.manager.GuiManager;
import org.lushplugins.lushlib.registry.RegistryUtils;
import org.lushplugins.lushlib.utils.*;
import org.lushplugins.lushlib.utils.converter.YamlConverter;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import org.lushplugins.rewardsapi.api.reward.Reward;

import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static LocalDate currentDate;

    private final ConcurrentHashMap<String, DisplayItemStack> categoryItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DisplayItemStack> globalItemTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Reward> rewardTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> messages = new ConcurrentHashMap<>();
    private boolean performanceMode;

    private boolean playtimeIgnoreAfk;
    private int reminderPeriod;
    private Sound reminderSound;
    private String defaultRewardGui;
    private boolean enableUpdater;

    public ConfigManager() {
        LushRewards plugin = LushRewards.getInstance();
        plugin.saveDefaultConfig();
        plugin.saveDefaultResource("reward-templates.yml");
    }

    public void reloadConfig() {
        LushRewards plugin = LushRewards.getInstance();
        plugin.getManager(GuiManager.class).ifPresent(GuiManager::closeAll);

        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));
        performanceMode = config.getBoolean("enable-performance-mode", false);
        if (performanceMode) {
            currentDate = LocalDate.now();
        }

        playtimeIgnoreAfk = config.getBoolean("playtime-ignore-afk", true);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        reminderSound = RegistryUtils.parseString(config.getString("reminder-sound", "none"), Registry.SOUNDS);
        defaultRewardGui = config.getString("default-reward-gui", "daily-rewards");

        enableUpdater = config.getBoolean("enable-updater", true);

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadRewardTemplates();
        reloadMessages(config.getConfigurationSection("messages"));
        plugin.getNotificationHandler().reloadNotifications();
    }

    public String getMessage(String messageName) {
        return getMessage(messageName, "");
    }

    public String getMessage(String messageName, String def) {
        String output = messages.getOrDefault(messageName, def);

        if (messages.containsKey("prefix")) {
            return output.replace("%prefix%", messages.get("prefix"));
        } else {
            return output;
        }
    }

    public DisplayItemStack getCategoryTemplate(String category) {
        DisplayItemStack itemTemplate = categoryItems.get(category.toLowerCase());
        if (itemTemplate == null) {
            LushRewards.getInstance().getLogger().severe("Could not find category '" + category + "'");
            return DisplayItemStack.empty();
        }

        return itemTemplate;
    }

    public DisplayItemStack getItemTemplate(String key, RewardModule module) {
        DisplayItemStack itemTemplate = module.getItemTemplate(key);
        return itemTemplate.isBlank() ? getItemTemplate(key) : itemTemplate;
    }

    public DisplayItemStack getItemTemplate(String key) {
        DisplayItemStack itemTemplate = globalItemTemplates.get(key);
        if (itemTemplate == null) {
            LushRewards.getInstance().getLogger().severe("Could not find item-template '" + key + "'");
            return DisplayItemStack.empty();
        }

        return itemTemplate;
    }

    public Reward getRewardTemplate(String name) {
        return rewardTemplates.get(name).clone();
    }

    public boolean isPerformanceModeEnabled() {
        return performanceMode;
    }

    public void checkRefresh() {
        if (performanceMode && !currentDate.isEqual(LocalDate.now())) {
            reloadConfig();
        }
    }

    public boolean shouldPlaytimeTrackerIgnoreAfk() {
        return playtimeIgnoreAfk;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public Sound getReminderSound() {
        return reminderSound;
    }

    public String getDefaultRewardGui() {
        return defaultRewardGui;
    }

    public boolean isUpdaterEnabled() {
        return enableUpdater;
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
                categoryItems.put(categorySection.getName(), YamlConverter.getDisplayItem(categorySection));
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
                globalItemTemplates.put(categorySection.getName(), YamlConverter.getDisplayItem(categorySection));
                LushRewards.getInstance().getLogger().info("Loaded global item-template: " + categorySection.getName());
            }
        });
    }

    private void reloadRewardTemplates() {
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(new File(LushRewards.getInstance().getDataFolder(), "reward-templates.yml"));
        } catch (IllegalArgumentException ignored) {
            return;
        }


        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");
        if (rewardsSection != null) {
            rewardsSection.getValues(false).forEach((key, value) -> {
                if (value instanceof ConfigurationSection rewardSection) {
                    Reward reward = RewardsAPI.readReward(rewardSection);
                    if (reward != null) {
                        rewardTemplates.put(rewardSection.getName(), reward);
                        LushRewards.getInstance().getLogger().info("Loaded reward-template: " + rewardSection.getName());
                    } else {
                        LushRewards.getInstance().getLogger().info("Failed to load reward-template: " + rewardSection.getName());
                    }
                }
            });
        }
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
