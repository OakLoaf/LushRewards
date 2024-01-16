package me.dave.activityrewarder.module.playtimetracker;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeTrackerModule extends Module {
    public static final String ID = "playtime-tracker";
    private ConcurrentHashMap<UUID, PlaytimeTracker> playtimeTrackers;
    private boolean poison;

    public PlaytimeTrackerModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        playtimeTrackers = new ConcurrentHashMap<>();
        poison = false;

        ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().runAtFixedRate(
                (task) -> {
                    if (poison || ActivityRewarder.getInstance().getModule(PlaytimeTrackerModule.ID).isEmpty()) {
                        task.cancel();
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
        poison = true;

        if (playtimeTrackers != null) {
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
