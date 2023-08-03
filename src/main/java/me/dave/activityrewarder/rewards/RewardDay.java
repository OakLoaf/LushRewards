package me.dave.activityrewarder.rewards;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RewardDay {
    private final List<RewardCollection> rewardCollections = new ArrayList<>();

    @Nullable
    public RewardCollection getHighestPriorityRewards() {
        return rewardCollections.stream().min(Comparator.comparingInt(RewardCollection::getPriority)).orElse(null);
    }
}
