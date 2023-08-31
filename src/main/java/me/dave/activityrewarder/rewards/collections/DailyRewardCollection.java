package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.exceptions.InvalidRewardException;
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
    private static DailyRewardCollection defaultReward = null;
    private final SimpleDate date;
    private final Integer streakDay;
    private final int repeatFrequency;
    private final SimpleDate repeatsUntil;

    public DailyRewardCollection(@Nullable SimpleDate date, @Nullable Integer streakDay, @Nullable Collection<Reward> rewards, int priority, int repeatFrequency, @Nullable SimpleDate repeatsUntil, @Nullable String category, @Nullable SimpleItemStack itemStack, @Nullable Sound sound) {
        super(rewards, priority, category, itemStack, sound);
        this.date = date;
        this.streakDay = streakDay;
        this.repeatFrequency = repeatFrequency;
        this.repeatsUntil = repeatsUntil;
    }

    @Nullable
    public SimpleDate getDate() {
        return date;
    }

    @Nullable
    public Integer getStreakDay() {
        return streakDay;
    }

    public int getRepeatFrequency() {
        return repeatFrequency;
    }

    public SimpleDate getRepeatsUntil() {
        return repeatsUntil;
    }

    public boolean shouldRepeat() {
        return repeatFrequency > 0;
    }

    public static DailyRewardCollection getDefaultReward() {
        return defaultReward;
    }

    public static void setDefaultReward(DailyRewardCollection defaultReward) {
        DailyRewardCollection.defaultReward = defaultReward;
    }

    @NotNull
    public static DailyRewardCollection from(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        SimpleDate rewardDate = null;
        Integer rewardDay = null;
        if (rewardCollectionSection.contains("on-date")) {
            rewardDate = SimpleDate.parse(rewardCollectionSection.getString("on-date", ""));
        }
        if (rewardCollectionSection.contains("on-day-num")) {
            rewardDay = rewardCollectionSection.getInt("on-day-num");
        }

        if (rewardDay == null && !rewardCollectionSection.getName().equalsIgnoreCase("default")) {
            throw new InvalidRewardException("Failed to find 'on-date' or 'on-day-num' at '" + rewardCollectionSection.getCurrentPath() + "'");
        }

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        int repeatFrequency = rewardCollectionSection.getInt("repeat", -1);
        Debugger.sendDebugMessage("Reward collection repeat frequency set to " + repeatFrequency, debugMode);

        SimpleDate repeatUntil = rewardCollectionSection.contains("repeats-until") ? SimpleDate.parse(rewardCollectionSection.getString("repeats-until")) : null;

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

        return rewardList != null ? new DailyRewardCollection(rewardDate, rewardDay, rewardList, priority, repeatFrequency, repeatUntil, category, itemStack, redeemSound) : DailyRewardCollection.empty();
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, null, null, 0, -1, null, null, null, null);
    }
}
