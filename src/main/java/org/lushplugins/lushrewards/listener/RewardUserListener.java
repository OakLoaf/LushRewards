package org.lushplugins.lushrewards.listener;

import org.bukkit.event.Listener;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTracker;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.Updater;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class RewardUserListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LushRewards.getInstance().getDataManager().getOrLoadRewardUser(player.getUniqueId()).thenAccept((rewardUser) -> {
            rewardUser.setUsername(player.getName());
            LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> ((PlaytimeTrackerModule) module).startPlaytimeTracker(player));
        });
        LushRewards.getInstance().getDataManager().loadModulesUserData(player.getUniqueId());

        if (player.hasPermission("lushrewards.update")) {
            Updater updater = LushRewards.getInstance().getUpdater();
            if (updater.isUpdateAvailable() && !updater.isAlreadyDownloaded()) {
                LushRewards.getMorePaperLib().scheduling().asyncScheduler().runDelayed(() -> ChatColorHandler.sendMessage(player, "&#ffe27aA new &#e0c01bLushRewards &#ffe27aupdate is now available, type &#e0c01b'/rewards update' &#ffe27ato download it!"), Duration.of(2, ChronoUnit.SECONDS));
            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);

        if (rewardUser != null) {
            LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> {
                PlaytimeTracker playtimeTracker = ((PlaytimeTrackerModule) module).stopPlaytimeTracker(uuid);
                if (playtimeTracker != null) {
                    rewardUser.setMinutesPlayed(playtimeTracker.getGlobalPlaytime());
                }
            });

            LushRewards.getInstance().getDataManager().saveRewardUser(rewardUser);
            LushRewards.getInstance().getDataManager().unloadRewardUser(uuid);
        }

        LushRewards.getInstance().getDataManager().saveModulesUserData(uuid);
        LushRewards.getInstance().getDataManager().unloadModulesUserData(uuid);
    }
}
