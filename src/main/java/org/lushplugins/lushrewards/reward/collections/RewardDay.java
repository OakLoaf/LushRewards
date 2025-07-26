package org.lushplugins.lushrewards.reward.collections;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.module.dailyrewards.DailyRewardCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class RewardDay {
    private final Collection<DailyRewardCollection> dailyRewardCollections = new ArrayList<>();

    @NotNull
    public DailyRewardCollection getHighestPriorityRewardCollection() {
        return dailyRewardCollections.stream().max(Comparator.comparingInt(DailyRewardCollection::getPriority)).orElse(DailyRewardCollection.empty());
    }

    public void giveAllRewards(Player player) {
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            dailyRewardCollection.giveAll(player);
        }
    }

    public void addCollection(DailyRewardCollection dailyRewardCollection) {
        dailyRewardCollections.add(dailyRewardCollection);
    }

    public void addCollections(Collection<DailyRewardCollection> dailyRewardCollections) {
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            addCollection(dailyRewardCollection);
        }
    }

    public boolean containsRewardFromCategory(String category) {
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            if (dailyRewardCollection.getCategory().equals(category)) return true;
        }

        return false;
    }

    public int getRewardCount() {
        int totalRewards = 0;
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            totalRewards += dailyRewardCollection.getRewardCount();
        }
        return totalRewards;
    }

    public boolean isEmpty() {
        return dailyRewardCollections.isEmpty();
    }

    @NotNull
    public static RewardDay from(Collection<DailyRewardCollection> dailyRewardCollections) {
        RewardDay rewardDay = new RewardDay();
        rewardDay.addCollections(dailyRewardCollections);
        return rewardDay;
    }

    @NotNull
    public static RewardDay from(DailyRewardCollection... dailyRewardCollections) {
        RewardDay rewardDay = new RewardDay();
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            rewardDay.addCollection(dailyRewardCollection);
        }
        return rewardDay;
    }
}
