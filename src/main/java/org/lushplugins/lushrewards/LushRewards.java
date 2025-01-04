package org.lushplugins.lushrewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.command.RewardsCommand;
import org.lushplugins.lushrewards.hook.FloodgateHook;
import org.lushplugins.lushrewards.hook.PlaceholderAPIHook;
import org.lushplugins.lushrewards.storage.migrator.Version3DataMigrator;
import org.lushplugins.lushrewards.module.RewardModuleTypeManager;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushrewards.notifications.NotificationHandler;
import org.lushplugins.lushrewards.rewards.RewardManager;
import org.lushplugins.lushrewards.utils.placeholder.LocalPlaceholders;
import org.lushplugins.lushrewards.utils.gson.LocalDateTypeAdapter;
import org.lushplugins.lushrewards.utils.gson.UserDataExclusionStrategy;
import org.bukkit.Bukkit;
import org.lushplugins.lushrewards.config.ConfigManager;
import org.lushplugins.lushrewards.data.DataManager;
import org.lushplugins.lushrewards.listener.RewardUserListener;
import org.bukkit.util.FileUtil;
import org.lushplugins.lushlib.LushLib;
import org.lushplugins.lushlib.plugin.SpigotPlugin;
import org.lushplugins.lushlib.utils.Updater;
import space.arim.morepaperlib.MorePaperLib;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class LushRewards extends SpigotPlugin {
    private static final Gson GSON;
    private static LushRewards plugin;
    private static MorePaperLib morePaperLib;

    private ConfigManager configManager;
    private DataManager dataManager;
    private NotificationHandler notificationHandler;
    private LocalPlaceholders localPlaceholders;
    private Updater updater;

    static {
        GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .addSerializationExclusionStrategy(new UserDataExclusionStrategy())
            .create();
    }

    @Override
    public void onLoad() {
        plugin = this;
        morePaperLib = new MorePaperLib(plugin);
        LushLib.getInstance().enable(this);

        registerManager(
            new RewardModuleTypeManager(),
            new RewardManager()
        );
    }

    @Override
    public void onEnable() {
        File oldDataFolder = new File(getDataFolder().getParentFile(), "ActivityRewarder");
        if (!getDataFolder().exists() && oldDataFolder.exists()) {
            if (FileUtil.copy(oldDataFolder, getDataFolder())) {
                File dataFolder = new File(getDataFolder(), "data");
                for (File file : dataFolder.listFiles()) {
                    file.delete();
                }
            }

            getLogger().info("Importing data from 'ActivityRewarder', this could take a moment");
            long start = Instant.now().toEpochMilli();
            try {
                new Version3DataMigrator().convert();
            } catch (FileNotFoundException e) {
                getLogger().severe("Failed to import data");
            }
            getLogger().info("Finished importing data (took " + (Instant.now().toEpochMilli() - start) + "ms)");
        }

        updater = new Updater(this, "djC8I9ui", "lushrewards.update", "rewards update");
        notificationHandler = new NotificationHandler();
        localPlaceholders = new LocalPlaceholders();

        configManager = new ConfigManager();
        configManager.reloadConfig();

        dataManager = new DataManager();
        dataManager.enable();

        addHook("floodgate", () -> registerHook(new FloodgateHook()));
        addHook("PlaceholderAPI", () -> registerHook(new PlaceholderAPIHook()));
        getHooks().forEach(Module::enable);

        new RewardUserListener().registerListeners();

        registerCommand(new RewardsCommand());

        getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> {
            if (module instanceof PlaytimeTrackerModule playtimeTracker) {
                Bukkit.getOnlinePlayers().forEach(playtimeTracker::startPlaytimeTracker);
            }
        });

        new Metrics(this, 22119);
    }

    @Override
    public void onDisable() {
        if (updater != null) {
            updater.shutdown();
            updater = null;
        }

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
            dataManager.disable();
            dataManager = null;
        }

        configManager = null;
        localPlaceholders = null;

        morePaperLib.scheduling().cancelGlobalTasks();
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

    public Gson getGson() {
        return GSON;
    }

    public List<RewardModule> getRewardModules() {
        return modules.values().stream()
            .filter(module -> module instanceof RewardModule)
            .map(module -> (RewardModule) module)
            .toList();
    }

    public List<RewardModule> getEnabledRewardModules() {
        return modules.values().stream()
            .filter(module -> module instanceof RewardModule && module.isEnabled())
            .map(module -> (RewardModule) module)
            .toList();
    }

    public static LushRewards getInstance() {
        return plugin;
    }

    public static MorePaperLib getMorePaperLib() {
        return morePaperLib;
    }
}
