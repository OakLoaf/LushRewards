package org.lushplugins.lushrewards.rewards.custom;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.exceptions.InvalidRewardException;
import org.lushplugins.lushrewards.rewards.Reward;
import org.lushplugins.lushrewards.utils.SchedulerType;
import org.lushplugins.lushlib.utils.RandomCollection;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("unused")
public class RandomReward extends Reward {
    private final RandomCollection<Reward> rewards;

    public RandomReward(RandomCollection<Reward> rewards) {
        this.rewards = rewards;
    }

    @SuppressWarnings("unchecked")
    public RandomReward(Map<?, ?> map) {
        super(map);
        List<Map<?, ?>> rewardMaps;
        try {
            rewardMaps = map.containsKey("rewards") ? (List<Map<?, ?>>) map.get("rewards") : List.of(Collections.emptyMap());
        } catch(ClassCastException exc) {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }

        this.rewards = new RandomCollection<>();

        rewardMaps.forEach((rewardMap) -> {
            Reward reward = Reward.loadReward(rewardMap, rewardMap.toString());
            int weight = rewardMap.containsKey("weight") ? (int) rewardMap.get("weight") : 1;
            if (reward != null) {
                rewards.add(reward, weight);
            }
        });
    }

    @Override
    protected void giveTo(Player player) {
        if (rewards != null && !rewards.isEmpty()) {
            Reward reward = rewards.next();

            try {
                reward.giveReward(player);
            } catch (Exception e) {
                LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" +reward.toString() + ") to " + player.getName());
                e.printStackTrace();
            }
        }
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();
        List<Map<String, Object>> rewardsMap = new ArrayList<>();
        rewards.getMap().forEach((weight, reward) -> {
            Map<String, Object> weightRewardMap = reward.asMap();
            weightRewardMap.put("weight", weight);

            rewardsMap.add(weightRewardMap);
        });

        rewardMap.put("type", "random");
        rewardMap.put("rewards", rewardsMap);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.ASYNC;
    }
}
