package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.rewards.Reward;
import me.dave.lushrewards.utils.SchedulerType;
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
