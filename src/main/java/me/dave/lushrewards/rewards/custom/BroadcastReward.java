package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.rewards.Reward;
import me.dave.lushrewards.utils.SchedulerType;
import org.bukkit.entity.Player;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class BroadcastReward extends Reward {
    private final String message;

    public  BroadcastReward(String message) {
        this.message = message;
    }

    public BroadcastReward(Map<?, ?> map) {
        this.message = (String) map.get("message");
    }

    @Override
    protected void giveTo(Player player) {
        ChatColorHandler.broadcastMessage(message.replaceAll("%player%", player.getDisplayName()));
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
