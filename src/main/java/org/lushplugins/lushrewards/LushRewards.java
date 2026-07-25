package org.lushplugins.lushrewards;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.guihandler.GuiHandler;
import org.lushplugins.guihandler.slot.SlotProvider;
import org.lushplugins.lushlib.libraries.jackson.databind.ObjectMapper;
import org.lushplugins.lushlib.libraries.jackson.databind.PropertyNamingStrategies;
import org.lushplugins.lushlib.libraries.jackson.dataformat.yaml.YAMLFactory;
import org.lushplugins.lushlib.serializer.JacksonHelper;
import org.lushplugins.lushrewards.command.EditUserCommand;
import org.lushplugins.lushrewards.command.RewardsCommand;
import org.lushplugins.lushrewards.reward.module.RewardModuleManager;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsPlaceholders;
import org.lushplugins.lushrewards.placeholder.Placeholders;
import org.lushplugins.lushrewards.storage.StorageManager;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.playtime.PlaytimeTrackerManager;
import org.lushplugins.lushrewards.notification.NotificationHandler;
import org.lushplugins.lushrewards.user.UserCache;
import org.lushplugins.lushrewards.utils.lamp.parametertype.LocalDateParameterType;
import org.lushplugins.lushrewards.utils.lamp.parametertype.MigratorParameterType;
import org.lushplugins.lushrewards.utils.lamp.parametertype.RewardModuleParameterType;
import org.lushplugins.lushrewards.utils.lamp.parametertype.RewardUserParameterType;
import org.lushplugins.lushrewards.utils.lamp.response.StringMessageResponseHandler;
import org.lushplugins.lushrewards.utils.gson.LocalDateTypeAdapter;
import org.lushplugins.lushrewards.utils.gson.UserDataExclusionStrategy;
import org.lushplugins.lushrewards.config.ConfigManager;
import org.lushplugins.lushlib.LushLib;
import org.lushplugins.lushlib.plugin.SpigotPlugin;
import org.lushplugins.lushrewards.utils.placeholderhandler.RewardModuleParameterProvider;
import org.lushplugins.placeholderhandler.PlaceholderHandler;
import org.lushplugins.pluginupdater.api.updater.Updater;
import org.lushplugins.pluginupdater.paper.api.PaperUpdater;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import org.lushplugins.rewardsapi.api.reward.RewardTypes;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import space.arim.morepaperlib.MorePaperLib;

import java.time.Duration;
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

    private GuiHandler guiHandler;
    private Updater<?> updater;
    private ConfigManager configManager;
    private RewardModuleManager rewardModuleManager;
    private PlaytimeTrackerManager playtimeTrackerManager;
    private NotificationHandler notificationHandler;
    private UserCache userCache;
    private StorageManager storageManager;
    private Lamp<BukkitCommandActor> lamp;

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
        this.guiHandler = GuiHandler.builder(this)
            .registerLabelProvider(' ', new SlotProvider())
            .build();

        this.playtimeTrackerManager = new PlaytimeTrackerManager();
        this.notificationHandler = new NotificationHandler();

        this.configManager = new ConfigManager();
        this.configManager.reloadConfig();

        this.rewardModuleManager = new RewardModuleManager();
        this.rewardModuleManager.reloadModules();

        this.userCache = new UserCache(this);
        this.storageManager = new StorageManager();

        if (configManager.isUpdaterEnabled()) {
            this.updater = PaperUpdater.builder(this)
                .modrinth(modrinth -> modrinth
                    .projectId("djC8I9ui"))
                .checkSchedule(600)
                .notify(true)
                .notificationPermission("lushrewards.update")
                .notificationMessage("&#ffe27aA new &#e0c01b%plugin% &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!")
                .build();
        }

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

        this.lamp = BukkitLamp.builder(this)
            .parameterTypes(parameterTypes -> parameterTypes
//                .addContextParameter(RewardUser.class, new RewardUserContextParameter()) // TODO: Temporarily disabled
                .addParameterType(LocalDate.class, new LocalDateParameterType())
                .addParameterType(Migrator.class, new MigratorParameterType())
                .addParameterType(RewardModule.class, new RewardModuleParameterType())
                .addParameterType(RewardUser.class, new RewardUserParameterType()))
            .responseHandler(String.class, new StringMessageResponseHandler())
            .build();
        this.lamp.register(
            new RewardsCommand(),
            new EditUserCommand()
        );

        PlaceholderHandler placeholderHandler = PlaceholderHandler.builder(this)
            .registerParameterProvider(String.class, (type, parameter, context) -> parameter)
            .registerParameterProvider(int.class, (type, parameter, context) -> Integer.parseInt(parameter))
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

        rewardModuleManager.getModules().forEach(RewardModule::onStartup);

        new Metrics(this, 22119);
    }

    @Override
    public void onDisable() {
        if (playtimeTrackerManager != null) {
            playtimeTrackerManager.disable();
            playtimeTrackerManager = null;
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

        configManager = null;

        RewardsAPI.getMorePaperLib().scheduling().cancelGlobalTasks();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RewardModuleManager getRewardModuleManager() {
        return rewardModuleManager;
    }

    public PlaytimeTrackerManager getPlaytimeTrackerManager() {
        return playtimeTrackerManager;
    }

    public NotificationHandler getNotificationHandler() {
        return notificationHandler;
    }

    public GuiHandler getGuiHandler() {
        return guiHandler;
    }

    public @Nullable Updater<?> getUpdater() {
        return updater;
    }

    public UserCache getUserCache() {
        return userCache;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return lamp;
    }

    public static LushRewards getInstance() {
        return plugin;
    }
}
