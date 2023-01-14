package me.dave.activityrewarder;

import me.dave.activityrewarder.events.RewardGUIEvents;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import me.dave.activityrewarder.datamanager.ConfigManager;
import me.dave.activityrewarder.datamanager.DataManager;
import me.dave.activityrewarder.events.RewardUserEvents;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static boolean hasFloodgate;
    public static DataManager dataManager;
    public static ConfigManager configManager;
    private final HashSet<UUID> guiPlayerSet = new HashSet<>();

    @Override
    public void onEnable() {
        plugin = this;
        saveDefaultConfig();
        configManager = new ConfigManager();
        dataManager = new DataManager();

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

        notifyPlayers();
    }

    @Override
    public void onDisable() {
        dataManager.getIoHandler().disableIOHandler();
    }

    private void notifyPlayers() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            LocalDate currDate = LocalDate.now();
           for (Player player : Bukkit.getOnlinePlayers()) {
               boolean collectedToday = currDate.equals(dataManager.getRewardUser(player.getUniqueId()).getLastDate());
               if (collectedToday) continue;
               ChatColorHandler.sendMessage(player, configManager.getReminderMessage());
               player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
           }
        }, 12000, 36000);
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
