package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.utils.RandomCollection;
import org.bukkit.entity.Player;

import java.util.*;

public class RandomReward implements Reward {
    private final RandomCollection<Reward> rewards;

    @SuppressWarnings("unchecked")
    public RandomReward(Map<?, ?> map) {
        List<Map<?, ?>> rewardMaps;
        try {
            rewardMaps = map.containsKey("rewards") ? (List<Map<?, ?>>) map.get("rewards") : List.of(Collections.emptyMap());
        } catch(ClassCastException exc) {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }

        this.rewards = new RandomCollection<>();

        rewardMaps.forEach((rewardMap) -> {
            Reward reward = Reward.loadReward(rewardMap, rewardMap.toString());
            double weight = rewardMap.containsKey("weight") ? (double) rewardMap.get("weight") : 1;
            if (reward != null) {
                rewards.add(weight, reward);
            }
        });
    }

    @Override
    public void giveTo(Player player) {
        if (rewards != null && !rewards.isEmpty()) {
            Reward reward = rewards.next();

            try {
                reward.giveTo(player);
            } catch (Exception e) {
                ActivityRewarder.getInstance().getLogger().severe(reward.toString());
                e.printStackTrace();
            }
        }
    }
}
