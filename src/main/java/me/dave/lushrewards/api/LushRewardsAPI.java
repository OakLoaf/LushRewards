package me.dave.lushrewards.api;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.rewards.RewardTypes;
import me.dave.lushrewards.rewards.custom.Reward;

@SuppressWarnings("unused")
public class LushRewardsAPI {

    public static void registerRewardModuleType(String typeId, RewardModule.Constructor<? extends RewardModule> constructor) {
        LushRewards.getInstance().registerModuleType(typeId, constructor);
    }

    public static void unregisterRewardModuleType(String typeId) {
        LushRewards.getInstance().unregisterModuleType(typeId);
    }

    public static void registerRewardType(String type, Class<? extends Reward> rewardClass) {
        RewardTypes.register(type, rewardClass);
    }

    public static void unregisterRewardType(String type) {
        RewardTypes.unregister(type);
    }
}
