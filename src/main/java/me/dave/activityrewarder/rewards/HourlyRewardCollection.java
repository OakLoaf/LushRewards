package me.dave.activityrewarder.rewards;

import java.util.List;

public record HourlyRewardCollection(double multiplier, List<Reward> rewardList) {}
