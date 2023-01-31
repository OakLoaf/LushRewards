package me.dave.activityrewarder.api;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.datamanager.RewardUser;
import me.dave.activityrewarder.rewards.RewardsDay;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private LocalDateTime nextDay = LocalDate.now().plusDays(1).atStartOfDay();

    public String onPlaceholderRequest(Player player, String params) {
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

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getIdentifier() {
        return "rewarder";
    }

    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
