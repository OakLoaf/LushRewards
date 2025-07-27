package org.lushplugins.lushrewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.command.RewardsCommand;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.hook.FloodgateHook;
import org.lushplugins.lushrewards.hook.PlaceholderAPIHook;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.migrator.Version3DataMigrator;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushrewards.notifications.NotificationHandler;
import org.lushplugins.lushrewards.utils.lamp.contextparameter.RewardUserContextParameter;
import org.lushplugins.lushrewards.utils.lamp.parametertype.MigratorParameterType;
import org.lushplugins.lushrewards.utils.lamp.parametertype.RewardModuleParameterType;
import org.lushplugins.lushrewards.utils.lamp.response.StringMessageResponseHandler;
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
import org.lushplugins.pluginupdater.api.updater.Updater;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import org.lushplugins.rewardsapi.api.reward.RewardTypes;
import revxrsal.commands.bukkit.BukkitLamp;
import space.arim.morepaperlib.MorePaperLib;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class LushRewards extends SpigotPlugin {
    private static final Gson GSON;
    private static LushRewards plugin;

    private Updater updater;
    private ConfigManager configManager;
    private DataManager dataManager;
    private NotificationHandler notificationHandler;
    private LocalPlaceholders localPlaceholders;

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
        LushLib.getInstance().enable(this);

        RewardsAPI.setMorePaperLib(new MorePaperLib(this));
        RewardsAPI.setLogger(this.getLogger());
        // TODO: Make TemplateReward class so that references don't break on reload
        RewardTypes.register("template", (map) -> {
            String template = (String) map.get("template");
            return LushRewards.getInstance().getConfigManager().getRewardTemplate(template);
        });
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

        this.updater = new Updater.Builder(this)
            .modrinth("djC8I9ui", true)
            .checkSchedule(600)
            .notify(true)
            .notificationPermission("lushrewards.update")
            .notificationMessage("&#ffe27aA new &#e0c01b%plugin% &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!")
            .build();

        notificationHandler = new NotificationHandler();
        localPlaceholders = new LocalPlaceholders();

        configManager = new ConfigManager();
        configManager.reloadConfig();

        dataManager = new DataManager();
        dataManager.enable();

        ifPluginEnabled("floodgate", () -> registerHook(new FloodgateHook()));
        ifPluginEnabled("PlaceholderAPI", () -> registerHook(new PlaceholderAPIHook()));
        getHooks().forEach(Module::enable);

        registerListener(new RewardUserListener());

//        registerCommand(new RewardsCommand());

        getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> {
            if (module instanceof PlaytimeTrackerModule playtimeTracker) {
                Bukkit.getOnlinePlayers().forEach(playtimeTracker::startPlaytimeTracker);
            }
        });

        RewardsAPI.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
            () -> {
                for (RewardModule module : LushRewards.getInstance().getEnabledRewardModules()) {
                    if (module instanceof PlaytimeRewardsModule playtimeModule) {
                        // TODO: Work out better way of running when a new day occurs
                        // eg. object that contains current date - when date changes then run check?
                        playtimeModule.checkAllOnlineForReset();
                    }
                }
            },
            Duration.of(1, ChronoUnit.MINUTES),
            Duration.of(1, ChronoUnit.MINUTES)
        );

        BukkitLamp.builder(this)
            .parameterTypes(parameterTypes -> parameterTypes
                .addContextParameter(RewardUser.class, new RewardUserContextParameter())
                .addParameterType(Migrator.class, new MigratorParameterType())
                .addParameterType(RewardModule.class, new RewardModuleParameterType()))
            .responseHandler(String.class, new StringMessageResponseHandler())
            .build()
            .register(new RewardsCommand());

        new Metrics(this, 22119);
    }

    @Override
    public void onDisable() {
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

        RewardsAPI.getMorePaperLib().scheduling().cancelGlobalTasks();
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
}
