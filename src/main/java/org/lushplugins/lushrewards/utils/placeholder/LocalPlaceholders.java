package org.lushplugins.lushrewards.utils.placeholder;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushlib.module.Module;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Either make shadeable Placeholder library or just add parseString method to parse messages/gui through
public class LocalPlaceholders {
    private static LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();

    private final ConcurrentHashMap<String, Placeholder> placeholders = new ConcurrentHashMap<>();

    public LocalPlaceholders() {

        registerPlaceholder(new SimplePlaceholder("countdown", (params, player) -> {
            LocalDateTime now = LocalDateTime.now();
            long secondsUntil = now.until(nextDay, ChronoUnit.SECONDS);

            if (secondsUntil < 0) {
                nextDay = LocalDate.now().plusDays(1).atStartOfDay();
                secondsUntil = now.until(nextDay, ChronoUnit.SECONDS);
            }

            long hours = secondsUntil / 3600;
            long minutes = (secondsUntil % 3600) / 60;
            long seconds = secondsUntil % 60;

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }));

        registerPlaceholder(new SimplePlaceholder("global_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getGlobalPlaytime());
            } else {
                return null;
            }
        }));

        registerPlaceholder(new SimplePlaceholder("session_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime());
            } else {
                return null;
            }
        }));

        registerPlaceholder(new SimplePlaceholder("total_session_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getTotalSessionPlaytime());
            } else {
                return null;
            }
        }));
    }

    public String parsePlaceholder(String params, Player player) {
        String[] paramsArr = params.split("_");

        Placeholder currentPlaceholder = null;
        String currParams = params;
        for (int i = 0; i < paramsArr.length; i++) {
            boolean found = false;

            for (Placeholder subPlaceholder : currentPlaceholder != null ? currentPlaceholder.getChildren() : placeholders.values()) {
                if (subPlaceholder.matches(currParams)) {
                    currentPlaceholder = subPlaceholder;
                    currParams = currParams.replace(subPlaceholder.getContent() + "_", "");

                    found = true;
                    break;
                }
            }

            if (!found) {
                break;
            }
        }

        if (currentPlaceholder != null) {
            try {
                return currentPlaceholder.parse(paramsArr, player);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public void registerPlaceholder(Placeholder placeholder) {
        placeholders.put(placeholder.getContent(), placeholder);
    }

    public void unregisterPlaceholder(String content) {
        placeholders.remove(content);
    }

    @FunctionalInterface
    public interface PlaceholderFunction {
        String apply(String[] params, Player player) ;
    }
}
