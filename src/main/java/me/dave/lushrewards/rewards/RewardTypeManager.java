package me.dave.lushrewards.rewards;

import me.dave.lushrewards.rewards.custom.*;
import me.dave.platyutils.manager.Manager;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class RewardTypeManager extends Manager {
    private ConcurrentHashMap<String, Reward.Constructor> rewardTypes;

    @Override
    public void onEnable() {
        rewardTypes = new ConcurrentHashMap<>();

        register("broadcast", BroadcastReward::new);
        register("command", ConsoleCommandReward::new);
        register("item", ItemReward::new);
        register("message", MessageReward::new);
        register("permission", PermissionReward::new);
        register("player-command", PlayerCommandReward::new);
        register("random", RandomReward::new);
    }

    @Override
    public void onDisable() {
        if (rewardTypes != null) {
            rewardTypes.clear();
            rewardTypes = null;
        }

    }

    public boolean isRegistered(String type) {
        return rewardTypes.containsKey(type);
    }

    public void register(String type, Reward.Constructor constructor) {
        rewardTypes.put(type, constructor);
    }

    public void unregister(String type) {
        rewardTypes.remove(type);
    }

    @Nullable
    public Reward.Constructor getConstructor(String type) {
        return rewardTypes.get(type);
    }
}
