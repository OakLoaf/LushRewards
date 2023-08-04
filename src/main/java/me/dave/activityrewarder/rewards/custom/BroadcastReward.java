package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;

import java.util.Map;

public class BroadcastReward implements Reward {
    private final String message;

    public BroadcastReward(String message) {
        this.message = message;
    }

    public BroadcastReward(Map<?, ?> map) {
        this.message = (String) map.get("message");
    }

    @Override
    public void giveTo(Player player) {
        ChatColorHandler.broadcastMessage(message);
    }
}
