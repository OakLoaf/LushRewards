package org.lushplugins.lushrewards.utils.placeholder;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import org.lushplugins.lushlib.module.Module;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

    public static class SimplePlaceholder extends Placeholder {
        private final PlaceholderFunction method;

        public SimplePlaceholder(String content, PlaceholderFunction method) {
            super(content);
            this.method = method;
        }

        @Override
        boolean matches(String string) {
            return string.startsWith(content);
        }

        @Override
        public String parse(String[] params, Player player) {
            try {
                return method.apply(params, player);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public SimplePlaceholder addChild(Placeholder placeholder) {
            super.addChild(placeholder);
            return this;
        }
    }

    public static class RegexPlaceholder extends Placeholder {
        private final PlaceholderFunction method;

        public RegexPlaceholder(String content, PlaceholderFunction method) {
            super(content);
            this.method = method;
        }

        @Override
        boolean matches(String string) {
            return string.matches(content);
        }

        @Override
        public String parse(String[] params, Player player) {
            try {
                return method.apply(params, player);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public RegexPlaceholder addChild(Placeholder placeholder) {
            super.addChild(placeholder);
            return this;
        }
    }

    public static abstract class Placeholder {
        protected final String content;
        private Collection<Placeholder> children;

        public Placeholder(String content) {
            this.content = content;
        }

        abstract boolean matches(String string);

        abstract String parse(String[] params, Player player);

        public String getContent() {
            return content;
        }

        @NotNull
        public Collection<Placeholder> getChildren() {
            return children != null ? children : Collections.emptyList();
        }

        public Placeholder addChild(Placeholder placeholder) {
            if (children == null) {
                children = new ArrayList<>();
            }

            children.add(placeholder);
            return this;
        }
    }
}
