package org.lushplugins.lushrewards.api;

import org.lushplugins.lushrewards.module.RewardModuleType;
import org.lushplugins.lushrewards.module.RewardModuleTypes;
import org.lushplugins.rewardsapi.api.reward.RewardType;
import org.lushplugins.rewardsapi.api.reward.RewardTypes;

@SuppressWarnings("unused")
public class LushRewardsAPI {

    public static void registerRewardModuleType(String id, RewardModuleType.Constructor<?> constructor) {
        RewardModuleTypes.register(id, constructor);
    }

    public static void unregisterRewardModuleType(String id) {
        RewardModuleTypes.unregister(id);
    }

    public static void registerRewardType(String type, RewardType.Constructor<?> constructor) {
        RewardTypes.register(type, constructor);
    }

    public static void unregisterRewardType(String type) {
        RewardTypes.unregister(type);
    }
}
