package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.hooks.FloodgateHook;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.module.ModuleType;
import me.dave.activityrewarder.module.RewardModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.plugin.SpigotPlugin;
import me.dave.platyutils.utils.Updater;
import org.bukkit.Bukkit;
import me.dave.activityrewarder.config.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.MorePaperLib;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ActivityRewarder extends SpigotPlugin {
    private static ActivityRewarder plugin;
    private static MorePaperLib morePaperLib;
    private ConfigManager configManager;
    private DataManager dataManager;
    private NotificationHandler notificationHandler;
    private Updater updater;
    private ConcurrentHashMap<String, RewardModule.CallableRewardModule<RewardModule>> moduleTypes;

    @Override
    public void onLoad() {
        plugin = this;

        moduleTypes = new ConcurrentHashMap<>();
        registerModuleType(ModuleType.DAILY_REWARDS, DailyRewardsModule::new);
//        registerModuleType(ModuleType.ONE_TIME_REWARDS, OneTimeRewardsModule::new);
        registerModuleType(ModuleType.PLAYTIME_REWARDS, PlaytimeDailyGoalsModule::new);
    }

    @Override
    public void onEnable() {
        updater = new Updater(this, "activity-rewarder", "activityrewarder.update", "rewards update");
        modules = new ConcurrentHashMap<>();

        morePaperLib = new MorePaperLib(plugin);
        notificationHandler = new NotificationHandler();
        configManager = new ConfigManager();
        configManager.reloadConfig();
        dataManager = new DataManager();

        addHook("floodgate", () -> registerHook(new FloodgateHook()));
        addHook("PlaceholderAPI", () -> registerHook(new PlaceholderAPIHook()));

        new RewardUserEvents().registerListeners();

        getCommand("rewards").setExecutor(new RewardCmd());

        Optional<Module> playtimeTracker = getModule(ModuleType.PLAYTIME_TRACKER);
        if (playtimeTracker.isPresent() && playtimeTracker.get() instanceof PlaytimeTrackerModule module) {
            Bukkit.getOnlinePlayers().forEach(module::startPlaytimeTracker);
        }
    }

    @Override
    public void onDisable() {
        updater.shutdown();

        if (notificationHandler != null) {
            notificationHandler.stopNotificationTask();
            notificationHandler = null;
        }

        if (hooks != null) {
            unregisterAllHooks();
            hooks = null;
        }

        if (modules != null) {
            unregisterAllModules();
            modules = null;
        }

        if (morePaperLib != null) {
            morePaperLib.scheduling().cancelGlobalTasks();
            morePaperLib = null;
        }

        if (dataManager != null) {
            dataManager.saveAll();
            dataManager.getIoHandler().disableIOHandler();
            dataManager = null;
        }

        configManager = null;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public NotificationHandler getNotificationHandler() {
        return notificationHandler;
    }

    public Updater getUpdater() {
        return updater;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasModuleType(@NotNull String type) {
        return moduleTypes.containsKey(type);
    }

    public RewardModule loadModuleType(@NotNull String type, @NotNull String moduleId, @NotNull File moduleFile) {
        type = type.toUpperCase();
        return moduleTypes.containsKey(type) ? moduleTypes.get(type).call(moduleId, moduleFile) : null;
    }

    public void registerModuleType(String id, RewardModule.CallableRewardModule<RewardModule> callable) {
        id = id.toUpperCase();

        if (moduleTypes.containsKey(id)) {
            log(Level.SEVERE, "Failed to register module type with id '" + id + "', a module type with this id is already registered");
        } else {
            moduleTypes.put(id, callable);
        }
    }

    public void unregisterModuleType(String id) {
        moduleTypes.remove(id);
    }

    public static ActivityRewarder getInstance() {
        return plugin;
    }

    public static MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }
}
