package me.dave.lushrewards.module.playtimetracker;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.libraries.paperlib.morepaperlib.scheduling.ScheduledTask;
import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeTrackerModule extends Module {
    private ConcurrentHashMap<UUID, PlaytimeTracker> playtimeTrackers;
    private ScheduledTask heartbeat;

    public PlaytimeTrackerModule() {
        super(RewardModule.Type.PLAYTIME_TRACKER);
    }

    @Override
    public void onEnable() {
        playtimeTrackers = new ConcurrentHashMap<>();

        heartbeat = PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
            () -> {
                if (LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).isEmpty()) {
                    heartbeat.cancel();
                    return;
                }

                playtimeTrackers.values().forEach(PlaytimeTracker::tick);
            },
            Duration.of(0, ChronoUnit.MILLIS),
            Duration.of(1000, ChronoUnit.MILLIS)
        );
    }

    @Override
    public void onDisable() {
        if (heartbeat != null) {
            heartbeat.cancel();
            heartbeat = null;
        }

        if (playtimeTrackers != null) {
            playtimeTrackers.values().forEach(PlaytimeTracker::saveData);
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
