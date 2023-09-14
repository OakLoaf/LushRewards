package me.dave.activityrewarder.events;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTracker;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
