package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.utils.SchedulerType;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class PermissionReward  extends Reward {
    private final String permission;
    private final List<Reward> rewards;

    public PermissionReward(String permission, List<Reward> rewards) {
        this.permission = permission;
        this.rewards = rewards;
    }

    @SuppressWarnings("unchecked")
    public PermissionReward(Map<?, ?> map) {
        List<Map<?, ?>> rewardMaps;
        permission = (String) map.get("permission");
        try {
            rewardMaps = map.containsKey("rewards") ? (List<Map<?, ?>>) map.get("rewards") : List.of(Collections.emptyMap());
        } catch(ClassCastException exc) {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }

        this.rewards = Reward.loadRewards(rewardMaps, map.toString());
    }

    @Override
    protected void giveTo(Player player) {
        if (player.hasPermission(permission)) {
            rewards.forEach(reward -> {
                try {
                    reward.giveReward(player);
                } catch (Exception e) {
                    ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" +reward.toString() + ") to " + player.getName());
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