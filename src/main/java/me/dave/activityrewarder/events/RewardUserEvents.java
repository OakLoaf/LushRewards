package me.dave.activityrewarder.events;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTracker;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.utils.Updater;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class RewardUserEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ActivityRewarder.getDataManager().getOrLoadRewardUser(player)
            .thenAccept((rewardUser) -> {
                rewardUser.setUsername(player.getName());

                if (ActivityRewarder.getModule(PlaytimeTrackerModule.ID) instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                    playtimeTrackerModule.startPlaytimeTracker(event.getPlayer());
                }
            });

        if (player.hasPermission("activityrewarder.update")) {
            Updater updater = ActivityRewarder.getInstance().getUpdater();
            if (updater.isUpdateAvailable() && !updater.isAlreadyDownloaded()) {
                ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().runDelayed(() -> {
                    ChatColorHandler.sendMessage(player, "&#ffe27aA new &#e0c01bActivityRewarder &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!");
                }, Duration.of(2, ChronoUnit.SECONDS));
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);

        if (ActivityRewarder.getModule(PlaytimeTrackerModule.ID) instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            PlaytimeTracker playtimeTracker = playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId());
            if (playtimeTracker != null) {
                rewardUser.setMinutesPlayed(playtimeTracker.getGlobalPlaytime());
            }
        }

        ActivityRewarder.getDataManager().saveRewardUser(rewardUser);
        ActivityRewarder.getDataManager().unloadRewarderUser(player.getUniqueId());
    }
}
