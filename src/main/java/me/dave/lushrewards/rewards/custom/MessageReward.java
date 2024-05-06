package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.rewards.Reward;
import me.dave.lushrewards.utils.SchedulerType;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Deprecated(since = "3.0.0")
@SuppressWarnings("unused")
public class MessageReward extends Reward {
    private final String message;

    public MessageReward(String message) {
        this.message = message;
    }

    public MessageReward(Map<?, ?> map) {
        this.message = (String) map.get("message");
        LushRewards.getInstance().getLogger().warning("Deprecated: Reward type 'message' is scheduled for removal, use the new 'message' option in any reward type");
    }

    @Override
    protected void giveTo(Player player) {
        ChatColorHandler.sendMessage(player, message.replace("%player%", player.getDisplayName()));
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
        return SchedulerType.ASYNC;
    }
}
