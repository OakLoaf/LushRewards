package org.enchantedskies.activityrewarder;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enchantedskies.activityrewarder.datamanager.ConfigManager;
import org.enchantedskies.activityrewarder.datamanager.DataManager;
import org.enchantedskies.activityrewarder.datamanager.Storage;
import org.enchantedskies.activityrewarder.events.RewardGUIEvents;
import org.enchantedskies.activityrewarder.events.RewardUserEvents;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

public final class ActivityRewarder extends JavaPlugin {
    private static ActivityRewarder plugin;
    private static boolean hasFloodgate;
    public static DataManager dataManager;
    public static ConfigManager configManager;
    private final HashSet<UUID> guiPlayerSet = new HashSet<>();

    private void setThreadIOName() {
        Storage.SERVICE.submit(() -> Thread.currentThread().setName("ActivityRewarder IO Thread"));
    }

    @Override
    public void onEnable() {
        plugin = this;
        setThreadIOName();
        saveDefaultConfig();
        configManager = new ConfigManager();
        dataManager = new DataManager();
        dataManager.initAsync((successful) -> {
            if (successful) {
                Listener[] listeners = new Listener[] {
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
            } else {
                Bukkit.getLogger().severe("Could not initialise the data. Aborting further plugin setup.");
            }
        });
    }

    @Override
    public void onDisable() {
        Storage.SERVICE.shutdownNow();
    }

    private void notifyPlayers() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            LocalDate currDate = LocalDate.now();
           for (Player player : Bukkit.getOnlinePlayers()) {
               boolean collectedToday = currDate.equals(dataManager.getRewardUser(player.getUniqueId()).getLatestDate());
               if (collectedToday) continue;
               player.sendMessage("§e§lRewards §8» §7It looks like you haven't collected today's reward from §e/rewards");
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
