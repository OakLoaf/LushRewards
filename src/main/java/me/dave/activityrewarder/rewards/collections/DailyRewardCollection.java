package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.exceptions.SimpleDateParseException;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DailyRewardCollection extends RewardCollection {
    private final Integer repeatFrequency;
    private final SimpleDate rewardDate;
    private final SimpleDate repeatsUntilDate;
    private final Integer rewardDayNum;
    private final Integer repeatsUntilDay;

    public DailyRewardCollection(@Nullable Integer repeatFrequency, @Nullable SimpleDate rewardDate, @Nullable SimpleDate repeatsUntilDate, @Nullable Integer rewardDayNum, @Nullable Integer repeatsUntilDay, @Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack itemStack, @Nullable Sound sound) {
        super(rewards, priority, category, itemStack, sound);
        this.repeatFrequency = repeatFrequency != null && repeatFrequency != 0 ? repeatFrequency : (repeatsUntilDay != null || repeatsUntilDate != null ? 1 : 0);
        this.rewardDate = rewardDate;
        this.repeatsUntilDate = repeatsUntilDate;
        this.rewardDayNum = rewardDayNum;
        this.repeatsUntilDay = repeatsUntilDay;
    }

    @Nullable
    public SimpleDate getRewardDate() {
        return rewardDate;
    }

    public boolean isAvailableOn(SimpleDate date) {
        if (rewardDate == null) {
            return false;
        }

        if (date.equals(rewardDate)) {
            return true;
        }

        if (repeatFrequency <= 0 || date.isAfter(rewardDate)) {
            return false;
        }

        if (repeatsUntilDate == null || !date.isBefore(repeatsUntilDate)) {
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

    @NotNull
    public static DailyRewardCollection from(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        SimpleDate rewardDate = null;
        Integer rewardDayNum = null;
        if (rewardCollectionSection.contains("on-date")) {
            rewardDate = SimpleDate.parse(rewardCollectionSection.getString("on-date", ""));
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
        SimpleDate repeatsUntilDate = null;

        if (rewardCollectionSection.contains("repeats-until")) {
            try {
                repeatsUntilDate = SimpleDate.parse(rewardCollectionSection.getString("repeats-until"));
            } catch (SimpleDateParseException e) {
                repeatsUntilDay = rewardCollectionSection.getInt("repeats-until", -1);
            }
        }

        String category = rewardCollectionSection.getString("category", "small");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        SimpleItemStack itemStack = itemSection != null ? SimpleItemStack.from(itemSection) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = ConfigParser.getSound(rewardCollectionSection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + ".rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return rewardList != null ? new DailyRewardCollection(repeatFrequency, rewardDate, repeatsUntilDate, rewardDayNum, repeatsUntilDay, rewardList, priority, category, itemStack, redeemSound) : DailyRewardCollection.empty();
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, null, null, null, null, null, 0, null, null, null);
    }
}
