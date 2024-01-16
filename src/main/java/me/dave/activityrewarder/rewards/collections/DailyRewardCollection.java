package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;

import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.utils.SimpleItemStack;
import me.dave.platyutils.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DailyRewardCollection extends RewardCollection {
    private final Integer repeatFrequency;
    private final LocalDate rewardDate;
    private final LocalDate repeatsUntilDate;
    private final Integer rewardDayNum;
    private final Integer repeatsUntilDay;

    public DailyRewardCollection(@Nullable Integer repeatFrequency, @Nullable LocalDate rewardDate, @Nullable LocalDate repeatsUntilDate, @Nullable Integer rewardDayNum, @Nullable Integer repeatsUntilDay, @Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack displayItem, @Nullable Sound sound) {
        super(rewards, priority, category, displayItem, sound);
        this.repeatFrequency = repeatFrequency != null && repeatFrequency != 0 ? repeatFrequency : (repeatsUntilDay != null || repeatsUntilDate != null ? 1 : 0);
        this.rewardDate = rewardDate;
        this.repeatsUntilDate = repeatsUntilDate;
        this.rewardDayNum = rewardDayNum;
        this.repeatsUntilDay = repeatsUntilDay;
    }

    @Nullable
    public LocalDate getRewardDate() {
        return rewardDate;
    }

    public boolean isAvailableOn(LocalDate date) {
        if (rewardDate == null) {
            return false;
        }

        if (date.equals(rewardDate)) {
            return true;
        }

        if (repeatFrequency <= 0 || date.isBefore(rewardDate)) {
            return false;
        }

        if (repeatsUntilDate == null || date.isBefore(repeatsUntilDate)) {
            // Checks if date is inline with repeating function
            return (date.toEpochDay() - rewardDate.toEpochDay()) % repeatFrequency == 0;
        } else {
            return false;
        }
    }

    @Nullable
    public Integer getRewardDayNum() {
        return rewardDayNum;
    }

    public boolean isAvailableOn(int dayNum) {
        if (rewardDayNum == null) {
            return false;
        }

        if (dayNum == rewardDayNum) {
            return true;
        }

        if (repeatFrequency <= 0 || dayNum < rewardDayNum) {
            return false;
        }

        if (repeatsUntilDay == null || dayNum <= repeatsUntilDay) {
            // Checks if dayNum is inline with repeating function
            return (dayNum - rewardDayNum) % repeatFrequency == 0;
        } else {
            return false;
        }
    }

    public void save(ConfigurationSection configurationSection) {
        if (rewardDate != null) {
            configurationSection.set("on-date", rewardDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }
        if (rewardDayNum != null) {
            configurationSection.set("on-day-num", rewardDayNum);
        }
        if (priority != 0) {
            configurationSection.set("priority", priority);
        }
        if (repeatFrequency != null && repeatFrequency != 0) {
            configurationSection.set("repeat", repeatFrequency);
        }
        if (repeatsUntilDay != null) {
            configurationSection.set("repeats-until", repeatsUntilDay);
        }
        if (repeatsUntilDate != null) {
            configurationSection.set("repeats-until", repeatsUntilDate);
        }
        if (category != null) {
            configurationSection.set("category", category);
        }
        if (displayItem != null && !displayItem.isBlank()) {
            displayItem.save(configurationSection.createSection("display-item"));
        }
        if (sound != null) {
            configurationSection.set("redeem-sound", sound.name());
        }
        if (!rewards.isEmpty()) {
            List<Map<String, Object>> rewardMaps = rewards.stream().map(Reward::asMap).toList();
            configurationSection.set("rewards", rewardMaps);
        }
    }

    @NotNull
    public static DailyRewardCollection from(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        LocalDate rewardDate = null;
        Integer rewardDayNum = null;
        if (rewardCollectionSection.contains("on-date")) {
            rewardDate = LocalDate.parse(rewardCollectionSection.getString("on-date", ""), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        }
        if (rewardCollectionSection.contains("on-day-num")) {
            rewardDayNum = rewardCollectionSection.getInt("on-day-num");
        }

        if (rewardDayNum == null && !rewardCollectionSection.getName().equalsIgnoreCase("default")) {
            throw new InvalidRewardException("Failed to find 'on-date' or 'on-day-num' at '" + rewardCollectionSection.getCurrentPath() + "'");
        }

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        int repeatFrequency = rewardCollectionSection.getInt("repeat", 0);
        Debugger.sendDebugMessage("Reward collection repeat frequency set to " + repeatFrequency, debugMode);

        Integer repeatsUntilDay = null;
        LocalDate repeatsUntilDate = null;

        if (rewardCollectionSection.contains("repeats-until")) {
            try {
                repeatsUntilDate = LocalDate.parse(rewardCollectionSection.getString("repeats-until"), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException e) {
                repeatsUntilDay = rewardCollectionSection.getInt("repeats-until", -1);
            }
        }

        String category = rewardCollectionSection.getString("category", "no-category");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        SimpleItemStack itemStack = itemSection != null ? SimpleItemStack.from(itemSection) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = StringUtils.getEnum(rewardCollectionSection.getString("redeem-sound", "none"), Sound.class).orElse(null);
        if (redeemSound == null) {
            Optional<Module> optionalModule = ActivityRewarder.getInstance().getModule(DailyRewardsModule.ID);
            if (optionalModule.isPresent()) {
                redeemSound = ((DailyRewardsModule) optionalModule.get()).getDefaultRedeemSound();
            }
        }

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + ".rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return new DailyRewardCollection(repeatFrequency, rewardDate, repeatsUntilDate, rewardDayNum, repeatsUntilDay, rewardList, priority, category, itemStack, redeemSound);
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, null, null, null, null, null, 0, null, null, null);
    }
}
