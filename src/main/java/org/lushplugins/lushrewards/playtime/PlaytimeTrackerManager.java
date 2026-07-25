package org.lushplugins.lushrewards.playtime;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlaytimeTrackerManager {
    public boolean enabled;
    private ConcurrentHashMap<UUID, PlaytimeTracker> playtimeTrackers;
    private ScheduledTask heartbeat;

    public boolean isEnabled() {
        return enabled;
    }

    public void ifEnabled(Consumer<PlaytimeTrackerManager> action) {
        if (enabled) {
            action.accept(this);
        }
    }

    public void enable() {
        if (enabled) {
            disable();
        }

        playtimeTrackers = new ConcurrentHashMap<>();

        heartbeat = RewardsAPI.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
            () -> playtimeTrackers.values().forEach(playtimeTracker -> {
                if (!playtimeTracker.tick()) {
                    this.stopPlaytimeTracker(playtimeTracker.getPlayer().getUniqueId());
                }
            }),
            Duration.ZERO,
            Duration.of(1000, ChronoUnit.MILLIS)
        );

        enabled = true;
        Bukkit.getOnlinePlayers().forEach(this::startPlaytimeTracker);
    }

    public void disable() {
        if (!enabled) {
            return;
        }

        enabled = false;

        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }

        if (playtimeTrackers != null) {
            for (PlaytimeTracker playtimeTracker : playtimeTrackers.values()) {
                LushRewards.getInstance().getUserCache().getUser(playtimeTracker.getPlayer().getUniqueId(), false).thenAccept(user -> {
                    user.setMinutesPlayed(playtimeTracker.getGlobalPlaytime());
                });
            }

            playtimeTrackers.clear();
            playtimeTrackers = null;
        }
    }

    public PlaytimeTracker getPlaytimeTracker(UUID uuid) {
        return playtimeTrackers.get(uuid);
    }

    public void startPlaytimeTracker(Player player) {
        playtimeTrackers.put(player.getUniqueId(), new PlaytimeTracker(player));
    }

    public PlaytimeTracker stopPlaytimeTracker(UUID uuid) {
        PlaytimeTracker playtimeTracker = getPlaytimeTracker(uuid);
        playtimeTrackers.remove(uuid);
        return playtimeTracker;
    }
}
