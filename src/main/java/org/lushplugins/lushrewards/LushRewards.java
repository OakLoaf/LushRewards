package org.lushplugins.lushrewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.lushplugins.lushlib.libraries.jackson.databind.ObjectMapper;
import org.lushplugins.lushlib.libraries.jackson.databind.PropertyNamingStrategies;
import org.lushplugins.lushlib.libraries.jackson.dataformat.yaml.YAMLFactory;
import org.lushplugins.lushlib.serializer.JacksonHelper;
import org.lushplugins.lushrewards.command.RewardsCommand;
import org.lushplugins.lushrewards.reward.module.RewardModuleManager;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.placeholder.DailyRewardsPlaceholders;
import org.lushplugins.lushrewards.placeholder.Placeholders;
import org.lushplugins.lushrewards.storage.StorageManager;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.migrator.Version3DataMigrator;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushrewards.notification.NotificationHandler;
import org.lushplugins.lushrewards.user.UserCache;
import org.lushplugins.lushrewards.utils.lamp.contextparameter.RewardUserContextParameter;
import org.lushplugins.lushrewards.utils.lamp.parametertype.MigratorParameterType;
import org.lushplugins.lushrewards.utils.lamp.parametertype.RewardModuleParameterType;
import org.lushplugins.lushrewards.utils.lamp.response.StringMessageResponseHandler;
import org.lushplugins.lushrewards.utils.placeholder.LocalPlaceholders;
import org.lushplugins.lushrewards.utils.gson.LocalDateTypeAdapter;
import org.lushplugins.lushrewards.utils.gson.UserDataExclusionStrategy;
import org.bukkit.Bukkit;
import org.lushplugins.lushrewards.config.ConfigManager;
import org.lushplugins.lushrewards.user.DataManager;
import org.lushplugins.lushrewards.listener.RewardUserListener;
import org.bukkit.util.FileUtil;
import org.lushplugins.lushlib.LushLib;
import org.lushplugins.lushlib.plugin.SpigotPlugin;
import org.lushplugins.lushrewards.utils.placeholderhandler.RewardModuleParameterProvider;
import org.lushplugins.placeholderhandler.PlaceholderHandler;
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

public final class LushRewards extends SpigotPlugin {
    public static final ObjectMapper YAML_MAPPER = JacksonHelper.addCustomSerializers(new ObjectMapper(new YAMLFactory())
        .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE));
    public static final ObjectMapper BASIC_JSON_MAPPER = new ObjectMapper();
    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
        .addSerializationExclusionStrategy(new UserDataExclusionStrategy())
        .create();

    private static LushRewards plugin;

    private Updater updater;
    private ConfigManager configManager;
    private RewardModuleManager rewardModuleManager;
    private DataManager dataManager; // TODO: Remove
    private NotificationHandler notificationHandler;
    private LocalPlaceholders localPlaceholders;
    private UserCache userCache;
    private StorageManager storageManager;

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

        this.notificationHandler = new NotificationHandler();
        this.localPlaceholders = new LocalPlaceholders();

        this.configManager = new ConfigManager();
        this.configManager.reloadConfig();

        this.rewardModuleManager = new RewardModuleManager();
        this.rewardModuleManager.reloadModules();

        this.userCache = new UserCache(this);
        this.storageManager = new StorageManager();

        this.dataManager = new DataManager();
        this.dataManager.enable();

        this.updater = new Updater.Builder(this)
            .modrinth("djC8I9ui", true)
            .checkSchedule(600)
            .notify(true)
            .notificationPermission("lushrewards.update")
            .notificationMessage("&#ffe27aA new &#e0c01b%plugin% &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!")
            .build();

        registerListener(new RewardUserListener());

        getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> {
            if (module instanceof PlaytimeTrackerModule playtimeTracker) {
                Bukkit.getOnlinePlayers().forEach(playtimeTracker::startPlaytimeTracker);
            }
        });

        RewardsAPI.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
            () -> {
                for (RewardModule module : LushRewards.getInstance().getRewardModuleManager().getModules()) {
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

        PlaceholderHandler placeholderHandler = PlaceholderHandler.builder(this)
            .registerParameterProvider(RewardUser.class, (type, parameter, context) -> {
                Player player = context.player();
                return player != null ? this.getUserCache().getCachedUser(player.getUniqueId()) : null;
            })
            .registerParameterProvider(RewardModule.class, new RewardModuleParameterProvider<>())
            .registerParameterProvider(DailyRewardsModule.class, new RewardModuleParameterProvider<>())
            .registerParameterProvider(PlaytimeRewardsModule.class, new RewardModuleParameterProvider<>())
            .build();
        placeholderHandler.register(new Placeholders());
        placeholderHandler.register(new DailyRewardsPlaceholders());

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

    public RewardModuleManager getRewardModuleManager() {
        return rewardModuleManager;
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

    public UserCache getUserCache() {
        return userCache;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public static LushRewards getInstance() {
        return plugin;
    }
}
