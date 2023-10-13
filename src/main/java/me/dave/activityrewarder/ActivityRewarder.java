package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.GuiEvents;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.Updater;
import me.dave.activityrewarder.utils.skullcreator.LegacySkullCreator;
import me.dave.activityrewarder.utils.skullcreator.NewSkullCreator;
import me.dave.activityrewarder.utils.skullcreator.SkullCreator;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.config.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;
import space.arim.morepaperlib.MorePaperLib;

import java.util.concurrent.ConcurrentHashMap;

public final class ActivityRewarder extends JavaPlugin {
    private static final SkullCreator skullCreator;
    private static ActivityRewarder plugin;
    private static ConcurrentHashMap<String, Module> modules;
    private static MorePaperLib morePaperLib;
    private static ConfigManager configManager;
    private static DataManager dataManager;
    private static NotificationHandler notificationHandler;
    private static boolean floodgateEnabled = false;
    private static PlaceholderAPIHook placeholderAPIHook = null;
    private Updater updater;

    static {
        String version = Bukkit.getBukkitVersion();
        if (version.contains("1.16") || version.contains("1.17") || version.contains("1.18")) {
            skullCreator = new LegacySkullCreator();
        } else {
            skullCreator = new NewSkullCreator();
        }
    }

    @Override
    public void onEnable() {
        plugin = this;
        updater = new Updater(this, "activity-rewarder", "rewards update");
        modules = new ConcurrentHashMap<>();

        morePaperLib = new MorePaperLib(plugin);
        notificationHandler = new NotificationHandler();
        configManager = new ConfigManager();
        configManager.reloadConfig();
        dataManager = new DataManager();

        Listener[] listeners = new Listener[] {
            new RewardUserEvents(),
            new GuiEvents()
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd());

        PluginManager pluginManager = getServer().getPluginManager();

        if (pluginManager.getPlugin("floodgate") != null) {
            floodgateEnabled = true;
            plugin.getLogger().info("Found plugin \"Floodgate\". Floodgate support enabled.");
        }

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook();
            placeholderAPIHook.register();
            plugin.getLogger().info("Found plugin \"PlaceholderAPI\". PlaceholderAPI support enabled.");
        }

        if (getModule(PlaytimeTrackerModule.ID) instanceof PlaytimeTrackerModule module) {
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

    public static Module getModule(String id) {
        return modules.get(id);
    }

    public static void registerModule(Module module) {
        modules.put(module.getId(), module);
        module.enable();
    }

    public static void unregisterModule(String moduleId) {
        Module module = modules.get(moduleId);
        if (module != null) {
            module.disable();
        }
        modules.remove(moduleId);
    }

    public static void unregisterAllModules() {
        modules.values().forEach(Module::disable);
        modules.clear();
    }

    public static ActivityRewarder getInstance() {
        return plugin;
    }

    public static MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }

    public static SkullCreator getSkullCreator() {
        return skullCreator;
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

    public static PlaceholderAPIHook getPlaceholderAPIHook() {
        return placeholderAPIHook;
    }
}
