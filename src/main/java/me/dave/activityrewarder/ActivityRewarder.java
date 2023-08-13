package me.dave.activityrewarder;

import me.dave.activityrewarder.commands.RewardCmd;
import me.dave.activityrewarder.config.RewardManager;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.GuiEvents;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.config.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;
import space.arim.morepaperlib.MorePaperLib;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static MorePaperLib morePaperLib;
    private static boolean hasFloodgate = false;
    private static ConfigManager configManager;
    private static RewardManager rewardManager;
    private static DataManager dataManager;

    @Override
    public void onEnable() {
        plugin = this;
        morePaperLib = new MorePaperLib(plugin);
        configManager = new ConfigManager();
        rewardManager = new RewardManager();
        dataManager = new DataManager();

        Listener[] listeners = new Listener[] {
            new RewardUserEvents(),
            new GuiEvents()
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd());

        PluginManager pluginManager = getServer().getPluginManager();

        if (pluginManager.getPlugin("floodgate") != null) {
            hasFloodgate = true;
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

    public static MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static RewardManager getRewardManager() {
        return rewardManager;
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static boolean isFloodgateEnabled() {
        return hasFloodgate;
    }
}
