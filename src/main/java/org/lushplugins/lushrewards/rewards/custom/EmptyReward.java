package org.lushplugins.lushrewards.rewards.custom;

import org.lushplugins.lushrewards.rewards.Reward;
import org.lushplugins.lushrewards.utils.SchedulerType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class EmptyReward extends Reward {

    public EmptyReward() {
        super();
    }

    public EmptyReward(Map<?, ?> map) {
        super(map);
    }

    @Override
    protected void giveTo(Player player) {}

    @Override
    protected SchedulerType getSchedulerType() {
        return SchedulerType.ASYNC;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();

        rewardMap.put("type", "empty");

        return rewardMap;
    }
}
