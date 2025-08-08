package org.lushplugins.lushrewards.config;

import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lushplugins.lushlib.manager.GuiManager;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushlib.utils.*;
import org.lushplugins.lushlib.utils.converter.YamlConverter;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.RewardModuleTypeManager;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushrewards.rewards.Reward;
import org.lushplugins.lushrewards.utils.Debugger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ConfigManager {
    private static final File MODULES_FOLDER = new File(LushRewards.getInstance().getDataFolder(), "modules");
    private static LocalDate currentDate;

    private final ConcurrentHashMap<String, DisplayItemStack> categoryItems = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DisplayItemStack> globalItemTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Reward> rewardTemplates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> messages = new ConcurrentHashMap<>();
    private boolean performanceMode;

    private boolean playtimeIgnoreAfk;
    private int reminderPeriod;
    private Sound reminderSound;

    public ConfigManager() {
        LushRewards plugin = LushRewards.getInstance();
        if (!new File(plugin.getDataFolder(), "config.yml").exists()) {
            plugin.saveDefaultResource("reward-templates.yml");
            plugin.saveDefaultResource("modules/daily-rewards.yml");
            plugin.saveDefaultResource("modules/daily-playtime-rewards.yml");
            plugin.saveDefaultResource("modules/global-playtime-rewards.yml");
        }

        plugin.saveDefaultConfig();
        plugin.saveDefaultResource("storage.yml");
    }

    public void reloadConfig() {
        LushRewards plugin = LushRewards.getInstance();
        plugin.getManager(GuiManager.class).ifPresent(GuiManager::closeAll);
        plugin.getRewardModules().forEach(module -> plugin.unregisterModule(module.getId()));

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
            Files.newDirectoryStream(MODULES_FOLDER.toPath(), "*.yml").forEach(entry -> {
                File moduleFile = entry.toFile();
                String moduleId = FilenameUtils.removeExtension(moduleFile.getName());
                YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(moduleFile);
                if (moduleConfig.getBoolean("enabled", true) && config.getBoolean("modules." + moduleId, true)) {
                    if (plugin.getModule(moduleId).isPresent()) {
                        plugin.log(Level.SEVERE, "A module with the id '" + moduleId + "' is already registered");
                        return;
                    }

                    String rewardsType;
                    if (moduleConfig.contains("type")) {
                        rewardsType = moduleConfig.getString("type");
                    } else if (moduleId.contains("playtime")) {
                        rewardsType = "playtime-rewards";
                    } else {
                        rewardsType = moduleId;
                    }

                    RewardModuleTypeManager rewardModuleTypes = LushRewards.getInstance().getManager(RewardModuleTypeManager.class).orElseThrow();
                    if (rewardsType != null && rewardModuleTypes.isRegistered(rewardsType)) {
                        plugin.registerModule(rewardModuleTypes.loadModuleType(rewardsType, moduleId, moduleFile));
                    } else {
                        plugin.log(Level.SEVERE, "Module with id '" + moduleId + "' failed to register due to invalid value at 'type'");
                    }
                }
            });
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Something went wrong whilst reading modules files", e);
        }

        boolean enableUpdater = config.getBoolean("enable-updater", true);
        plugin.getUpdater().setEnabled(enableUpdater);
        if (enableUpdater) {
            plugin.getUpdater().queueCheck();
        }

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadRewardTemplates();
        reloadMessages(config.getConfigurationSection("messages"));
        plugin.getNotificationHandler().reloadNotifications();

        if (plugin.getDataManager() != null) {
            plugin.getDataManager().reloadRewardUsers(true);
        }

        plugin.getRewardModules().forEach(Module::reload);

        if (plugin.getEnabledRewardModules().stream().anyMatch(RewardModule::requiresPlaytimeTracker)) {
            if (LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).isEmpty()) {
                PlaytimeTrackerModule playtimeTrackerModule = new PlaytimeTrackerModule();
                plugin.registerModule(playtimeTrackerModule);
                playtimeTrackerModule.enable();
            }
        } else {
            plugin.unregisterModule(RewardModule.Type.PLAYTIME_TRACKER);
        }
    }



    public String getMessage(String messageName) {
        String def;
        if (messageName.equals("confirm-command")) {
            def = "&#ffe27aAre you sure you want to do that? Type &#e0c01b'%command%' &#ffe27ato confirm";
        } else {
            def = "";
        }

        return getMessage(messageName, def);
    }

    public String getMessage(String messageName, String def) {
        String output = messages.getOrDefault(messageName, def);

        if (messages.containsKey("prefix")) {
            return output.replace("%prefix%", messages.get("prefix"));
        } else {
            return output;
        }
    }

    public Collection<String> getMessages() {
        return messages.values();
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
                    Reward reward = Reward.loadReward(rewardSection);
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
