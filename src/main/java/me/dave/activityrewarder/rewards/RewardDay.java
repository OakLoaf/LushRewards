package me.dave.activityrewarder.rewards;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class RewardDay {
    private final Collection<DailyRewardCollection> dailyRewardCollections = new ArrayList<>();

    @Nullable
    public DailyRewardCollection getHighestPriorityRewards() {
        return dailyRewardCollections.stream().min(Comparator.comparingInt(DailyRewardCollection::priority)).orElse(null);
    }

    public void addCollection(DailyRewardCollection rewardCollection) {
        dailyRewardCollections.add(rewardCollection);
    }

    public void addCollections(Collection<DailyRewardCollection> rewardCollections) {
        rewardCollections.forEach(this::addCollection);
    }

    public boolean containsRewardFromCategory(String category) {
        for (DailyRewardCollection dailyRewardCollection : dailyRewardCollections) {
            if (dailyRewardCollection.category().equals(category)) return true;
        }
        return false;
    }

    public static RewardDay from(Collection<DailyRewardCollection> rewardCollections) {
        RewardDay rewardDay = new RewardDay();
        rewardDay.addCollections(rewardCollections);
        return rewardDay;
    }

    public static RewardDay from(DailyRewardCollection... rewardCollections) {
        RewardDay rewardDay = new RewardDay();
        for (DailyRewardCollection rewardCollection : rewardCollections) {
            rewardDay.addCollection(rewardCollection);
        }
        return rewardDay;
    }
}
