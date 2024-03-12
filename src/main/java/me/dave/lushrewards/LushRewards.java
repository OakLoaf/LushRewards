package me.dave.lushrewards;

import me.dave.lushrewards.commands.RewardsCommand;
import me.dave.lushrewards.hook.FloodgateHook;
import me.dave.lushrewards.hook.PlaceholderAPIHook;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsModule;
import me.dave.lushrewards.module.playtimegoals.PlaytimeGoalsModule;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.lushrewards.notifications.NotificationHandler;
import me.dave.lushrewards.utils.LocalPlaceholders;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.plugin.SpigotPlugin;
import me.dave.platyutils.utils.Updater;
import org.bukkit.Bukkit;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.data.DataManager;
import me.dave.lushrewards.listener.RewardUserListener;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class LushRewards extends SpigotPlugin {
    private static LushRewards plugin;

    private ConfigManager configManager;
    private DataManager dataManager;
    private NotificationHandler notificationHandler;
    private LocalPlaceholders localPlaceholders;
    private Updater updater;
    private ConcurrentHashMap<String, RewardModule.Constructor<? extends RewardModule>> moduleTypes;

    @Override
    public void onLoad() {
        plugin = this;
        PlatyUtils.enable(this);

        moduleTypes = new ConcurrentHashMap<>();
        registerModuleType(RewardModule.Type.DAILY_REWARDS, DailyRewardsModule::new);
//        registerModuleType(ModuleType.ONE_TIME_REWARDS, OneTimeRewardsModule::new);
        registerModuleType(RewardModule.Type.PLAYTIME_REWARDS, PlaytimeGoalsModule::new);
    }

    @Override
    public void onEnable() {
        updater = new Updater(this, "lush-rewards", "lushrewards.update", "rewards update");

        notificationHandler = new NotificationHandler();
        configManager = new ConfigManager();
        configManager.reloadConfig();
        dataManager = new DataManager();
        localPlaceholders = new LocalPlaceholders();

        addHook("floodgate", () -> registerHook(new FloodgateHook()));
        addHook("PlaceholderAPI", () -> registerHook(new PlaceholderAPIHook()));

        new RewardUserListener().registerListeners();

        registerCommand(new RewardsCommand());

        Optional<Module> playtimeTracker = getModule(RewardModule.Type.PLAYTIME_TRACKER);
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

        if (dataManager != null) {
            dataManager.saveAll();
            dataManager.getIoHandler().disableIOHandler();
            dataManager = null;
        }

        configManager = null;
        localPlaceholders = null;
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

    public LocalPlaceholders getLocalPlaceholders() {
        return localPlaceholders;
    }

    public Updater getUpdater() {
        return updater;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasModuleType(@NotNull String type) {
        return moduleTypes.containsKey(type);
    }

    public List<? extends RewardModule> getRewardModules() {
        return modules.values().stream()
            .filter(module -> module instanceof RewardModule)
            .map(module -> (RewardModule) module)
            .toList();
    }

    public RewardModule loadModuleType(@NotNull String type, @NotNull String moduleId, @NotNull File moduleFile) {
        type = type.toUpperCase();
        return moduleTypes.containsKey(type) ? moduleTypes.get(type).build(moduleId, moduleFile) : null;
    }

    public void registerModuleType(String id, RewardModule.Constructor<? extends RewardModule> constructor) {
        if (moduleTypes.containsKey(id)) {
            log(Level.SEVERE, "Failed to register module type with id '" + id + "', a module type with this id is already registered");
        } else {
            moduleTypes.put(id, constructor);
        }
    }

    public void unregisterModuleType(String id) {
        moduleTypes.remove(id);
    }

    public static LushRewards getInstance() {
        return plugin;
    }
}
