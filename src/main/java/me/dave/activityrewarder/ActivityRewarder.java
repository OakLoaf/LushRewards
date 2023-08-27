package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.GuiEvents;
import me.dave.activityrewarder.module.Module;
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
    private static final ConcurrentHashMap<String, Module> modules = new ConcurrentHashMap<>();
    private static ActivityRewarder plugin;
    private static MorePaperLib morePaperLib;
    private static boolean floodgateEnabled = false;
    private static ConfigManager configManager;
    private static DataManager dataManager;

    @Override
    public void onEnable() {
        plugin = this;
        morePaperLib = new MorePaperLib(plugin);
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
        } else {
            plugin.getLogger().info("Floodgate plugin not found. Continuing without floodgate.");
        }

        if (pluginManager.getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook().register();
        } else {
            plugin.getLogger().info("PlaceholderAPI plugin not found. Continuing without PlaceholderAPI.");
        }
    }

    @Override
    public void onDisable() {
        dataManager.saveAll();
        modules.clear();
        dataManager.getIoHandler().disableIOHandler();
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

    public static Module getModule(String id) {
        return modules.get(id);
    }

    public static void registerModule(Module module) {
        modules.put(module.getId(), module);
        module.enable();
    }

    public static void unregisterAll() {
        modules.values().forEach(Module::disable);
        modules.clear();
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

    public static boolean isFloodgateEnabled() {
        return floodgateEnabled;
    }
}
