package me.dave.lushrewards.utils;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.module.ModuleType;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsModule;
import me.dave.lushrewards.module.playtimedailygoals.PlaytimeDailyGoalsModule;
import me.dave.lushrewards.module.playtimeglobalgoals.PlaytimeGlobalGoalsModule;
import me.dave.lushrewards.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import me.dave.lushrewards.module.playtimetracker.PlaytimeTrackerModule;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardDay;
import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalPlaceholders {
    private static final String identifier = "rewarder";
    private static final ConcurrentHashMap<String, PlaceholderFunction<String>> stringPlaceholders = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, PlaceholderFunction<String>> regexPlaceholders = new ConcurrentHashMap<>();
    private static final Pattern regexPattern = Pattern.compile("%" + identifier + "_([a-zA-Z0-9_ ]+)%");
    private static LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();

    static {
        // String placeholders
        registerPlaceholder("category", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(DailyRewardsModule.ID);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule dailyRewardsModule) {
                RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
                if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData)) {
                    return null;
                }

                RewardDay rewardDay = dailyRewardsModule.getRewardDay(LocalDate.now(), moduleUserData.getStreakLength());

                return String.valueOf(rewardDay.getHighestPriorityRewardCollection().getCategory());
            } else {
                return null;
            }
        });

        registerPlaceholder("collected", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData)) {
                return null;
            }

            return String.valueOf(moduleUserData.hasCollectedToday());
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

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData)) {
                return null;
            }
            
            return String.valueOf(moduleUserData.getDayNum());
        });

        registerPlaceholder("global_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(ModuleType.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getGlobalPlaytime());
            } else {
                return null;
            }
        });

        registerPlaceholder("highest_streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData)) {
                return null;
            }

            return String.valueOf(moduleUserData.getHighestStreak());
        });

        registerPlaceholder("playtime_since_daily_goals", (params, player) -> {
            if (player == null || LushRewards.getInstance().getModule(PlaytimeDailyGoalsModule.ID).isEmpty()) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);

            if (rewardUser.getModuleData(PlaytimeDailyGoalsModule.ID) instanceof PlaytimeDailyGoalsModule.UserData moduleData) {
                return String.valueOf(rewardUser.getMinutesPlayed() - moduleData.getLastCollectedPlaytime());
            } else {
                return null;
            }
        });

        registerPlaceholder("playtime_since_global_goals", (params, player) -> {
            if (player == null || LushRewards.getInstance().getModule(PlaytimeDailyGoalsModule.ID).isEmpty()) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);

            if (rewardUser.getModuleData(PlaytimeGlobalGoalsModule.ID) instanceof PlaytimeGoalsModuleUserData moduleData) {
                return String.valueOf(rewardUser.getMinutesPlayed() - moduleData.getLastCollectedPlaytime());
            } else {
                return null;
            }
        });

        registerPlaceholder("session_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(ModuleType.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime());
            } else {
                return null;
            }
        });

        registerPlaceholder("streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData)) {
                return null;
            }

            return String.valueOf(moduleUserData.getStreakLength());
        });

        registerPlaceholder("total_rewards", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(DailyRewardsModule.ID);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule dailyRewardsModule) {
                RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
                if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData userData)) {
                    return null;
                }

                RewardDay rewardDay = dailyRewardsModule.getRewardDay(LocalDate.now(), userData.getStreakLength());

                return String.valueOf(rewardDay.getRewardCount());
            } else {
                return null;
            }
        });

        registerPlaceholder("total_session_playtime", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalPlaytimeTracker = LushRewards.getInstance().getModule(ModuleType.PLAYTIME_TRACKER);
            if (optionalPlaytimeTracker.isPresent() && optionalPlaytimeTracker.get() instanceof PlaytimeTrackerModule playtimeTrackerModule) {
                return String.valueOf(playtimeTrackerModule.getPlaytimeTracker(player.getUniqueId()).getTotalSessionPlaytime());
            } else {
                return null;
            }
        });

        // Regex Placeholders
        registerRegexPlaceholder("day_[0-9]+.+", (params, player) -> {
            Optional<Module> optionalModule = LushRewards.getInstance().getModule(DailyRewardsModule.ID);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule dailyRewardsModule) {
                RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
                if (!(rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData userData)) {
                    return null;
                }

                String[] paramArr = params.split("_", 3);
                int dayNum = Integer.parseInt(paramArr[1]);

                LocalDate date = userData.getDateAtStreakLength(dayNum);

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
        R apply(String string, Player player) ;
    }
}
