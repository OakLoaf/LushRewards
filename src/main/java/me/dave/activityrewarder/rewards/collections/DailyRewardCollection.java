package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class DailyRewardCollection extends RewardCollection {
    private final int priority;
    private final String category;
    private final SimpleItemStack itemStack;
    private final Sound sound;

    private DailyRewardCollection(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack itemStack, @Nullable Sound sound) {
        super(rewards);
        this.priority = priority;
        this.category = category != null ? category : ActivityRewarder.getRewardManager().getDefaultReward().getCategory();
        this.itemStack = itemStack;
        this.sound = sound != null ? sound : ActivityRewarder.getRewardManager().getDefaultReward().getSound();
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
        return itemStack != null ? itemStack : ActivityRewarder.getConfigManager().getCategoryItem(category);
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
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
        return new DailyRewardCollection(null, 0, null, null, null);
    }
}
