package me.dave.activityrewarder.events;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.ModuleType;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTracker;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.listener.EventListener;
import me.dave.platyutils.utils.Updater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class RewardUserEvents implements EventListener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ActivityRewarder.getInstance().getDataManager().getOrLoadRewardUser(player)
            .thenAccept((rewardUser) -> {
                rewardUser.setUsername(player.getName());

                ActivityRewarder.getInstance().getModule(ModuleType.PLAYTIME_TRACKER).ifPresent(module -> {
                    ((PlaytimeTrackerModule) module).startPlaytimeTracker(player);
                });
            });

        if (player.hasPermission("activityrewarder.update")) {
            Updater updater = ActivityRewarder.getInstance().getUpdater();
            if (updater.isUpdateAvailable() && !updater.isAlreadyDownloaded()) {
                PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().runDelayed(() -> {
                    ChatColorHandler.sendMessage(player, "&#ffe27aA new &#e0c01bActivityRewarder &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!");
                }, Duration.of(2, ChronoUnit.SECONDS));
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RewardUser rewardUser = ActivityRewarder.getInstance().getDataManager().getRewardUser(player);

        ActivityRewarder.getInstance().getModule(ModuleType.PLAYTIME_TRACKER).ifPresent(module -> {
            PlaytimeTracker playtimeTracker = ((PlaytimeTrackerModule) module).stopPlaytimeTracker(player.getUniqueId());
            if (playtimeTracker != null) {
                rewardUser.setMinutesPlayed(playtimeTracker.getGlobalPlaytime());
            }
        });

        ActivityRewarder.getInstance().getDataManager().saveRewardUser(rewardUser);
        ActivityRewarder.getInstance().getDataManager().unloadRewarderUser(player.getUniqueId());
    }
}
