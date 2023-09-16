package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.utils.SchedulerType;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class MessageReward extends Reward {
    private final String message;

    public MessageReward(String message) {
        this.message = message;
    }

    public MessageReward(Map<?, ?> map) {
        this.message = (String) map.get("message");
    }

    @Override
    protected void giveTo(Player player) {
        ChatColorHandler.sendMessage(player, message);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();

        rewardMap.put("type", "message");
        rewardMap.put("message", message);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.PLAYER;
    }
}
