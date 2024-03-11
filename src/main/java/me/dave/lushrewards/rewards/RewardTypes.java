package me.dave.lushrewards.rewards;

import me.dave.lushrewards.rewards.custom.*;

import java.util.concurrent.ConcurrentHashMap;

public class RewardTypes {
    private static final ConcurrentHashMap<String, Class<? extends Reward>> nameToRewardType = new ConcurrentHashMap<>();

    static {
        register("broadcast", BroadcastReward.class);
        register("command", ConsoleCommandReward.class);
        register("item", ItemReward.class);
        register("message", MessageReward.class);
        register("permission", PermissionReward.class);
        register("player-command", PlayerCommandReward.class);
        register("random", RandomReward.class);
    }

    public static void register(String type, Class<? extends Reward> rewardClass) {
        nameToRewardType.put(type, rewardClass);
    }

    public static boolean isRewardRegistered(String type) {
        return nameToRewardType.containsKey(type);
    }

    public static Class<? extends Reward> getClass(String type) {
        return nameToRewardType.get(type);
    }
}
