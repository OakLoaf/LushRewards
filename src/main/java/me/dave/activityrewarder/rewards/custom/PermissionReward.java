package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.exceptions.InvalidRewardException;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PermissionReward  implements Reward {
    private final String permission;
    private final List<Reward> rewards;

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
    public void giveTo(Player player) {
        if (player.hasPermission(permission)) {
            rewards.forEach(reward -> {
                try {
                    reward.giveTo(player);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}