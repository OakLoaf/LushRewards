package org.lushplugins.lushrewards.rewards.custom;

import org.lushplugins.lushrewards.exceptions.InvalidRewardException;
import org.lushplugins.lushrewards.rewards.Reward;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class WrapperReward extends Reward {
    protected final List<Reward> rewards;

    public WrapperReward(List<Reward> rewards) {
        this.rewards = rewards;
    }

    @SuppressWarnings("unchecked")
    public WrapperReward(Map<?, ?> map) {
        super(map);
        List<Map<?, ?>> rewardMaps;
        try {
            rewardMaps = map.containsKey("rewards") ? (List<Map<?, ?>>) map.get("rewards") : List.of(Collections.emptyMap());
        } catch(ClassCastException exc) {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }

        this.rewards = rewardMaps.stream().map(rewardMap -> Reward.loadReward(rewardMap, rewardMap.toString())).toList();
    }
}
