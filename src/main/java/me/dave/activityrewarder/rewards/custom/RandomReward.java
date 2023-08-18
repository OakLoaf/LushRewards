package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomReward implements Reward {
    private static final Random random = new Random();
    private final List<Reward> rewards;

    @SuppressWarnings("unchecked")
    public RandomReward(Map<?, ?> map) {
        List<Map<?, ?>> rewardMaps;
        try {
            rewardMaps = map.containsKey("rewards") ? (List<Map<?, ?>>) map.get("rewards") : List.of(Collections.emptyMap());
        } catch(ClassCastException exc) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "'");
            throw new InvalidRewardException();
        }

        // TODO: replace #loadRewards() with local alternative that takes into account weights
        this.rewards = Reward.loadRewards(rewardMaps, map.toString());
    }

    @Override
    public void giveTo(Player player) {
        if (rewards != null && !rewards.isEmpty()) {
            rewards.get(random.nextInt(0, rewards.size())).giveTo(player);
        }
    }
}
