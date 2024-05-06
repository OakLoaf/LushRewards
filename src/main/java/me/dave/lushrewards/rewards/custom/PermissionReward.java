package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.rewards.Reward;
import me.dave.lushrewards.utils.SchedulerType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class PermissionReward extends WrapperReward {
    private final String permission;

    public PermissionReward(String permission, List<Reward> rewards) {
        super(rewards);
        this.permission = permission;
    }

    public PermissionReward(Map<?, ?> map) {
        super(map);
        permission = (String) map.get("permission");
    }

    @Override
    protected void giveTo(Player player) {
        if (player.hasPermission(permission)) {
            rewards.forEach(reward -> {
                try {
                    reward.giveReward(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" +reward.toString() + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();

        rewardMap.put("type", "permission");
        rewardMap.put("permission", permission);

        List<Map<String, Object>> rewardsMap = rewards.stream().map(Reward::asMap).toList();
        rewardMap.put("rewards", rewardsMap);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.ASYNC;
    }
}