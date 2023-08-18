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
            rewardDate = SimpleDate.from(rewardCollectionSection.getString("on-date", ""));
        } else if (rewardCollectionSection.contains("on-streak-day")) {
            rewardDay = rewardCollectionSection.getInt("on-streak-day");
        } else {
            throw new InvalidRewardException("Failed to find 'on-date' or 'on-streak-day' at '" + rewardCollectionSection.getCurrentPath() + "'");
        }

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

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

        return rewardList != null ? new DailyRewardCollection(rewardDate, rewardDay, rewardList, 0, category, itemStack, redeemSound) : DailyRewardCollection.empty();
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, null, null, 0, null, null, null);
    }
}
