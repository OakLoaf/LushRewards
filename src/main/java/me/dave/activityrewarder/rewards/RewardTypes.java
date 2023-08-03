package me.dave.activityrewarder.rewards;

import me.dave.activityrewarder.rewards.custom.BroadcastReward;
import me.dave.activityrewarder.rewards.custom.CommandReward;
import me.dave.activityrewarder.rewards.custom.ItemReward;
import me.dave.activityrewarder.rewards.custom.MessageReward;

import java.util.HashMap;

public class RewardTypes {
    private static final HashMap<String, Class<? extends Reward>> nameToRewardType = new HashMap<>();

    static {
        register("broadcast", BroadcastReward.class);
        register("command", CommandReward.class);
        register("item", ItemReward.class);
        register("message", MessageReward.class);
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
