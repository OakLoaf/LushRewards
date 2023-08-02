package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.GuiEvents;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.config.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static boolean folia = false;
    private static boolean hasFloodgate = false;
    public static DataManager dataManager;
    public static ConfigManager configManager;

    @Override
    public void onEnable() {
        plugin = this;
        configManager = new ConfigManager();
        dataManager = new DataManager();

        Listener[] listeners = new Listener[] {
            new RewardUserEvents(),
            new GuiEvents()
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd());

        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.getPlugin("floodgate") != null) hasFloodgate = true;
        else plugin.getLogger().info("Floodgate plugin not found. Continuing without floodgate.");

        if (pluginManager.getPlugin("PlaceholderAPI") != null) new PlaceholderAPIHook().register();
        else plugin.getLogger().info("PlaceholderAPI plugin not found. Continuing without PlaceholderAPI.");


        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }

        ChatColorHandler.enableMiniMessage(true);
    }

    @Override
    public void onDisable() {
        dataManager.getIoHandler().disableIOHandler();
    }

    private void registerEvents(Listener[] listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    public static ActivityRewarder getInstance() {
        return plugin;
    }

    public static boolean isRunningOnFolia() {
        return folia;
    }

    public static boolean isFloodgateEnabled() {
        return hasFloodgate;
    }
}
