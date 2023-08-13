package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.rewards.custom.Reward;

import java.util.Collection;

public class PlaytimeRewardCollection extends RewardCollection {
    private final double multiplier;

    public PlaytimeRewardCollection(double multiplier, Collection<Reward> rewards) {
        super(rewards);
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }
}
