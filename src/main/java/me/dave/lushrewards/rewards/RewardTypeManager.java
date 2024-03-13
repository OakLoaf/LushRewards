package me.dave.lushrewards.rewards;

import me.dave.lushrewards.rewards.custom.*;
import me.dave.platyutils.manager.Manager;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class RewardTypeManager extends Manager {
    private ConcurrentHashMap<String, Reward.Constructor> nameToRewardType;

    @Override
    public void onEnable() {
        nameToRewardType = new ConcurrentHashMap<>();

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
        if (nameToRewardType != null) {
            nameToRewardType.clear();
            nameToRewardType = null;
        }

    }

    public boolean isRegistered(String type) {
        return nameToRewardType.containsKey(type);
    }

    public void register(String type, Reward.Constructor constructor) {
        nameToRewardType.put(type, constructor);
    }

    public void unregister(String type) {
        nameToRewardType.remove(type);
    }

    @Nullable
    public Reward.Constructor getConstructor(String type) {
        return nameToRewardType.get(type);
    }
}
