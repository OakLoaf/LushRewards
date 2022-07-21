package org.enchantedskies.activityrewarder.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.enchantedskies.activityrewarder.datamanager.RewardUser;

public class RewardUserEvents implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ActivityRewarder.dataManager.loadRewardUser(player.getUniqueId()).thenAccept((ignored) -> {
            RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
            rewardUser.setUsername(player.getName());
        });
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        ActivityRewarder.dataManager.saveRewardUser(rewardUser);
    }
}
