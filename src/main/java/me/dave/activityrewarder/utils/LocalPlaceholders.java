package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.rewards.RewardsDay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalPlaceholders {
    private static final String identifier = "rewarder";
    private static final Pattern regexPattern = Pattern.compile("%" + identifier + "_[a-zA-Z0-9_ ]+%");
    private static LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();


    public static ItemStack parseItemStack(Player player, ItemStack itemStack) {
        ItemStack item = itemStack.clone();
        ItemMeta itemMeta = item.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(parseString(player, itemMeta.getDisplayName()));

            List<String> lore = itemMeta.getLore();
            if (lore != null) {
                List<String> newLore = new ArrayList<>();
                for (String loreLine : lore) {
                    newLore.add(parseString(player, loreLine));
                }
                itemMeta.setLore(newLore);
            }
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    public static String parseString(Player player, String string) {
        Matcher matcher = regexPattern.matcher(string);
        Set<String> matches = new HashSet<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        for (String match : matches) {
            String parsed = parsePlaceholder(player, match);
            if (parsed == null) continue;
            string = string.replaceAll(match, parsed);
        }

        return string;
    }

    public static String parsePlaceholder(Player player, String params) {
        // Global placeholders
        if (params.equals("countdown")) {
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
        }

        // Player placeholders
        if (player != null) {
            RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
            switch (params) {
                case "day_num" -> {
                    if (rewardUser.hasCollectedToday()) return String.valueOf(rewardUser.getDayNum() - 1);
                    else return String.valueOf(rewardUser.getDayNum() - 1);
                }
                case "collected" -> {
                    return String.valueOf(rewardUser.hasCollectedToday());
                }
                case "playtime" -> {
                    return String.valueOf(rewardUser.getPlayTimeSinceLastCollected());
                }
                case "multiplier" -> {
                    return String.valueOf(rewardUser.getHourlyMultiplier());
                }
                case "size" -> {
                    return String.valueOf(ActivityRewarder.configManager.getRewards(rewardUser.getActualDayNum()).getSize());
                }
                case "total_rewards" -> {
                    return String.valueOf(ActivityRewarder.configManager.getRewards(rewardUser.getActualDayNum()).getRewardCount());
                }
            }
        }

        // Day placeholders
        if (params.matches("day_[0-9]+.+")) {
            String[] paramArr = params.split("_", 3);
            int dayNum = Integer.parseInt(paramArr[1]);
            RewardsDay rewardsDay = ActivityRewarder.configManager.getRewards(dayNum);

            switch(paramArr[2]) {
                case "size" -> {
                    return String.valueOf(rewardsDay.getSize());
                }
                case "total_rewards" -> {
                    return String.valueOf(rewardsDay.getRewardCount());
                }
            }

            return null;
        }

        return null;
    }
}
