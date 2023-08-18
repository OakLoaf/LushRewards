package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.ActivityRewarder;
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
    private final int priority;
    private final String category;
    private final SimpleItemStack itemStack;
    private final Sound sound;

    public DailyRewardCollection(@Nullable SimpleDate date, @Nullable Integer streakDay, @Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack itemStack, @Nullable Sound sound) {
        super(rewards);
        this.date = date;
        this.streakDay = streakDay;
        this.priority = priority;
        this.category = category != null ? category : defaultReward.getCategory();
        this.itemStack = itemStack;
        this.sound = sound != null ? sound : defaultReward.getSound();
    }

    @Nullable
    public SimpleDate getDate() {
        return date;
    }

    @Nullable
    public Integer getStreakDay() {
        return streakDay;
    }

    public int getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public Sound getSound() {
        return sound;
    }

    public SimpleItemStack getDisplayItem() {
        return itemStack != null ? itemStack : ActivityRewarder.getConfigManager().getCategoryTemplate(category);
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static DailyRewardCollection getDefaultReward() {
        return defaultReward;
    }

    public static void setDefaultReward(DailyRewardCollection defaultReward) {
        DailyRewardCollection.defaultReward = defaultReward;
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack itemStack, @Nullable Sound sound) {
        return new DailyRewardCollection(rewards, priority, category, itemStack, sound);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack itemStack) {
        return new DailyRewardCollection(rewards, priority, category, itemStack, null);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category) {
        return new DailyRewardCollection(rewards, priority, category, null, null);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority) {
        return new DailyRewardCollection(rewards, priority, null, null, null);
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, null, null, 0, null, null, null);
    }
}
