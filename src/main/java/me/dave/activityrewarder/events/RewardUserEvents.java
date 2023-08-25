package me.dave.activityrewarder.events;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
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
        ActivityRewarder.getDataManager().loadRewardUser(player.getUniqueId()).thenAccept((rewardUser) -> rewardUser.setUsername(player.getName()));

        if (ActivityRewarder.getModule("playtime-tracker") instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            playtimeTrackerModule.startPlaytimeTracker(player);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);

        if (ActivityRewarder.getModule("playtime-tracker") instanceof PlaytimeTrackerModule playtimeTrackerModule) {
            rewardUser.setPlayMinutes(playtimeTrackerModule.stopPlaytimeTracker(player.getUniqueId()).getGlobalTime());
        }

        ActivityRewarder.getDataManager().saveRewardUser(rewardUser);
        ActivityRewarder.getDataManager().unloadRewarderUser(player.getUniqueId());
    }
}
