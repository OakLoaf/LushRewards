package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.utils.SchedulerType;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class BroadcastReward extends Reward {
    private final String message;

    public  BroadcastReward(String message) {
        this.message = message;
    }

    public BroadcastReward(Map<?, ?> map) {
        this.message = (String) map.get("message");
    }

    @Override
    public void giveTo(Player player) {
        ChatColorHandler.broadcastMessage(message);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();

        rewardMap.put("type", "broadcast");
        rewardMap.put("message", message);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.ASYNC;
    }
}
