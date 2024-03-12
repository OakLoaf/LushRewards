package me.dave.lushrewards.api;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;

@SuppressWarnings("unused")
public class LushRewardsAPI {

    public static void registerRewardModuleType(String typeId, RewardModule.Constructor<? extends RewardModule> constructor) {
        LushRewards.getInstance().registerModuleType(typeId, constructor);
    }

    public static void unregisterRewardModuleType(String typeId) {
        LushRewards.getInstance().unregisterModuleType(typeId);
    }
}
