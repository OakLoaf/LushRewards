package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalPlaceholders {
    private static final String identifier = "rewarder";
    private static final HashMap<String, PlaceholderFunction<String>> stringPlaceholders = new HashMap<>();
    private static final HashMap<String, PlaceholderFunction<String>> regexPlaceholders = new HashMap<>();
    private static final Pattern regexPattern = Pattern.compile("%" + identifier + "_([a-zA-Z0-9_ ]+)%");
    private static LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();

    static {
        // String placeholders
        registerPlaceholder("category", (params, player) -> {
            if (!(ActivityRewarder.getModule("daily-rewards") instanceof DailyRewardsModule dailyRewardsModule) || player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            RewardDay rewardDay = dailyRewardsModule.getRewardDay(SimpleDate.now(), rewardUser.getStreakLength());

            return String.valueOf(rewardDay.getHighestPriorityRewardCollection().getCategory());
        });

        registerPlaceholder("collected", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            return String.valueOf(rewardUser.hasCollectedToday());
        });

        registerPlaceholder("countdown", (params, player) -> {
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
        });

        registerPlaceholder("day_num", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            return String.valueOf(rewardUser.getDayNum());
        });

        registerPlaceholder("global_playtime", (params, player) -> {
            if (!(ActivityRewarder.getModule("playtime-tracker") instanceof PlaytimeTrackerModule playtimeTrackerModule) || player == null) {
                return null;
            }

            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getGlobalPlaytime());
        });

        registerPlaceholder("highest_streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            return String.valueOf(rewardUser.getHighestStreak());
        });

        registerPlaceholder("multiplier", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            return String.valueOf(rewardUser.getHighestStreak());
        });

        // TODO: Replace with a placeholder for each goal module
//        registerPlaceholder("playtime", (params, player) -> {
//            if (player == null) {
//                return null;
//            }
//
//            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
//            return String.valueOf(rewardUser.getPlayTimeSinceLastCollected());
//        });

        registerPlaceholder("session_playtime", (params, player) -> {
            if (!(ActivityRewarder.getModule("playtime-tracker") instanceof PlaytimeTrackerModule playtimeTrackerModule) || player == null) {
                return null;
            }

            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime());
        });

        registerPlaceholder("streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            return String.valueOf(rewardUser.getStreakLength());
        });

        registerPlaceholder("total_rewards", (params, player) -> {
            if (!(ActivityRewarder.getModule("daily-rewards") instanceof DailyRewardsModule dailyRewardsModule) || player == null) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            RewardDay rewardDay = dailyRewardsModule.getRewardDay(SimpleDate.now(), rewardUser.getStreakLength());

            return String.valueOf(rewardDay.getRewardCount());
        });

        registerPlaceholder("total_session_playtime", (params, player) -> {
            if (!(ActivityRewarder.getModule("playtime-tracker") instanceof PlaytimeTrackerModule playtimeTrackerModule) || player == null) {
                return null;
            }

            return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getTotalSessionPlaytime());
        });

        // Regex Placeholders
        registerRegexPlaceholder("day_[0-9]+.+", (params, player) -> {
            if (!(ActivityRewarder.getModule("daily-rewards") instanceof DailyRewardsModule dailyRewardsModule)) {
                return null;
            }

            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);

            String[] paramArr = params.split("_", 3);
            int dayNum = Integer.parseInt(paramArr[1]);

            SimpleDate date = rewardUser.getDateAtStreakLength(dayNum);

            RewardDay rewardDay = dailyRewardsModule.getRewardDay(date, dayNum);

            DailyRewardCollection dailyRewardCollection = rewardDay.getHighestPriorityRewardCollection();

            switch (paramArr[2]) {
                case "category" -> {
                    return String.valueOf(dailyRewardCollection.getCategory());
                }
                case "total_rewards" -> {
                    return String.valueOf(dailyRewardCollection.getRewardCount());
                }
            }

            return null;
        });
    }

    public static ItemStack parseItemStack(Player player, ItemStack itemStack) {
        ItemStack item = itemStack.clone();
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(parseString(itemMeta.getDisplayName(), player));

            List<String> lore = itemMeta.getLore();
            if (lore != null) {
                List<String> newLore = new ArrayList<>();
                for (String loreLine : lore) {
                    newLore.add(parseString(loreLine, player));
                }
                itemMeta.setLore(newLore);
            }
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    public static String parseString(String string, Player player) {
        Matcher matcher = regexPattern.matcher(string);
        Set<String> matches = new HashSet<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        for (String match : matches) {
            String parsed = parsePlaceholder(match, player);
            if (parsed == null) {
                continue;
            }
            string = string.replaceAll(match, parsed);
        }

        return string;
    }

    public static String parsePlaceholder(String params, Player player) {

        if (stringPlaceholders.containsKey(params)) {
            try {
                return stringPlaceholders.get(params).apply(params, player);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        for (String regex : regexPlaceholders.keySet()) {
            if (params.matches(regex)) {
                try {
                    return regexPlaceholders.get(params).apply(params, player);
                } catch(Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }

        return null;
    }

    public static void registerPlaceholder(String placeholder, PlaceholderFunction<String> method) {
        stringPlaceholders.put(placeholder, method);
    }

    public static void registerRegexPlaceholder(String regex, PlaceholderFunction<String> method) {
        regexPlaceholders.put(regex, method);
    }

    @FunctionalInterface
    private interface PlaceholderFunction<R> {
        R apply(String string, Player player);
    }
}
