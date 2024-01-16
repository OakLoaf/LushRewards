package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.GuiEvents;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.plugin.SpigotPlugin;
import me.dave.platyutils.utils.Updater;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import me.dave.activityrewarder.config.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;
import space.arim.morepaperlib.MorePaperLib;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ActivityRewarder extends SpigotPlugin {
    private static ActivityRewarder plugin;
    private static ConcurrentHashMap<String, Module> modules;
    private static MorePaperLib morePaperLib;
    private static ConfigManager configManager;
    private static DataManager dataManager;
    private static NotificationHandler notificationHandler;
    private static boolean floodgateEnabled = false;
    private Updater updater;

    @Override
    public void onLoad() {
        plugin = this;
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

        addHook("floodgate", () -> floodgateEnabled = true);
        addHook("PlaceholderAPI", () -> registerHook(new PlaceholderAPIHook()));

        Listener[] listeners = new Listener[] {
                new RewardUserEvents(),
                new GuiEvents()
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd());

        Optional<Module> playtimeTracker = getModule(PlaytimeTrackerModule.ID);
        if (playtimeTracker.isPresent() && playtimeTracker.get() instanceof PlaytimeTrackerModule module) {
            Bukkit.getOnlinePlayers().forEach(module::startPlaytimeTracker);
        }
    }

    @Override
    public void onDisable() {
        updater.shutdown();

        if (morePaperLib != null) {
            morePaperLib.scheduling().cancelGlobalTasks();
            morePaperLib = null;
        }

        if (notificationHandler != null) {
            notificationHandler.stopNotificationTask();
            notificationHandler = null;
        }

        if (dataManager != null) {
            dataManager.saveAll();
            dataManager.getIoHandler().disableIOHandler();
            dataManager = null;
        }

        if (modules != null) {
            unregisterAllModules();
            modules = null;
        }

        configManager = null;
    }

    public boolean callEvent(Event event) {
        getServer().getPluginManager().callEvent(event);
        if (event instanceof Cancellable cancellable) {
            return !cancellable.isCancelled();
        } else {
            return true;
        }
    }

    public void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public Updater getUpdater() {
        return updater;
    }

    public static ActivityRewarder getInstance() {
        return plugin;
    }

    public static MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static NotificationHandler getNotificationHandler() {
        return notificationHandler;
    }

    public static boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }
}
