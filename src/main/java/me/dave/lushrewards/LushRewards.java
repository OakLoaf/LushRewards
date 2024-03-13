package me.dave.lushrewards;

import me.dave.lushrewards.commands.RewardsCommand;
import me.dave.lushrewards.hook.FloodgateHook;
import me.dave.lushrewards.hook.PlaceholderAPIHook;
import me.dave.lushrewards.module.RewardModuleTypeManager;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.lushrewards.notifications.NotificationHandler;
import me.dave.lushrewards.rewards.RewardTypeManager;
import me.dave.lushrewards.utils.LocalPlaceholders;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.plugin.SpigotPlugin;
import me.dave.platyutils.utils.Updater;
import org.bukkit.Bukkit;
import me.dave.lushrewards.config.ConfigManager;
import me.dave.lushrewards.data.DataManager;
import me.dave.lushrewards.listener.RewardUserListener;

import java.util.List;
import java.util.Optional;

public final class LushRewards extends SpigotPlugin {
    private static LushRewards plugin;

    private ConfigManager configManager;
    private DataManager dataManager;
    private NotificationHandler notificationHandler;
    private LocalPlaceholders localPlaceholders;
    private Updater updater;

    @Override
    public void onLoad() {
        plugin = this;
        PlatyUtils.enable(this);
    }

    @Override
    public void onEnable() {
        PlatyUtils.registerManager(
            new RewardModuleTypeManager(),
            new RewardTypeManager()
        );

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

        PlatyUtils.disable();
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

    public List<? extends RewardModule> getRewardModules() {
        return modules.values().stream()
            .filter(module -> module instanceof RewardModule)
            .map(module -> (RewardModule) module)
            .toList();
    }

    public static LushRewards getInstance() {
        return plugin;
    }
}
