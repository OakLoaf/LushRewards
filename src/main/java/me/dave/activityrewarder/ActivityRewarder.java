package me.dave.activityrewarder;

import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.events.RewardGuiEvents;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.data.ConfigManager;
import me.dave.activityrewarder.data.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;

import java.util.HashSet;
import java.util.UUID;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static boolean hasFloodgate = false;
    public static DataManager dataManager;
    public static ConfigManager configManager;
    private final HashSet<UUID> guiPlayerSet = new HashSet<>();

    @Override
    public void onEnable() {
        plugin = this;
        configManager = new ConfigManager();
        dataManager = new DataManager();

        Listener[] listeners = new Listener[] {
            new RewardUserEvents(),
            new RewardGuiEvents(guiPlayerSet)
        };
        registerEvents(listeners);

        getCommand("rewards").setExecutor(new RewardCmd(guiPlayerSet));

        PluginManager pluginManager = getServer().getPluginManager();
        if (pluginManager.getPlugin("floodgate") != null) hasFloodgate = true;
        else plugin.getLogger().info("Floodgate plugin not found. Continuing without floodgate.");

        if (pluginManager.getPlugin("PlaceholderAPI") != null) new PlaceholderAPIHook().register();
        else plugin.getLogger().info("PlaceholderAPI plugin not found. Continuing without PlaceholderAPI.");

//        new UpdateChecker(this, UpdateCheckSource.SPIGET, "107545")
//            .setDownloadLink("https://www.spigotmc.org/resources/activity-rewarder.107545/")
//            .setChangelogLink("https://www.spigotmc.org/resources/activity-rewarder.107545/updates")
//            .onSuccess((commandSenders, latestVersion) -> {
//                String currentVersion = getDescription().getVersion();
//                if (latestVersion == currentVersion) return;
//                for (CommandSender sender : (CommandSender[]) commandSenders) {
//                    ChatColorHandler.sendMessage(sender, "&e&lRewards &8» &7Version &e" + latestVersion + "&7is available!");
//                    ChatColorHandler.sendMessage(sender, "&e&lRewards &8» &7You are currently running on " + currentVersion);
//                    ChatColorHandler.sendMessage(sender, "&e&lRewards &8» &7Download the latest version here:");
//                    ChatColorHandler.sendMessage(sender, "&e&lRewards &8» &7https://www.spigotmc.org/resources/activity-rewarder.107545");
//                }
//            })
//            .onFail((commandSenders, latestVersion) -> {
//                getLogger().warning("Error when getting latest version");
//            })
//            .setNotifyByPermissionOnJoin("activityrewarder.updatechecker")
//            .setNotifyRequesters(false)
//            .suppressUpToDateMessage(true)
//            .checkEveryXHours(0.5)
//            .checkNow();
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

    public static boolean isFloodgateEnabled() {
        return hasFloodgate;
    }
}
