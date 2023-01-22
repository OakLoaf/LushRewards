package me.dave.activityrewarder;

import me.dave.activityrewarder.events.RewardGUIEvents;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.datamanager.ConfigManager;
import me.dave.activityrewarder.datamanager.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;

import java.util.HashSet;
import java.util.UUID;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static boolean hasFloodgate;
    public static DataManager dataManager;
    public static ConfigManager configManager;
    public static NotificationHandler notificationHandler;
    private final HashSet<UUID> guiPlayerSet = new HashSet<>();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        configManager = new ConfigManager();
        dataManager = new DataManager();
        notificationHandler = new NotificationHandler();

        Listener[] listeners = new Listener[]{
            new RewardUserEvents(),
            new RewardGUIEvents(guiPlayerSet)
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd(guiPlayerSet));

        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.getPlugin("floodgate") != null) hasFloodgate = true;
        else {
            hasFloodgate = false;
            getLogger().info("Floodgate plugin not found. Continuing without floodgate.");
        }
    }

    @Override
    public void onDisable() {
        dataManager.getIoHandler().disableIOHandler();
    }

    public static ActivityRewarder getInstance() {
        return plugin;
    }

    public static boolean isFloodgateEnabled() {
        return hasFloodgate;
    }

    public void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
