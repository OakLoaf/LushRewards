package me.dave.activityrewarder.api;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.RewardModule;

@SuppressWarnings("unused")
public class ActivityRewarderAPI {

    public static void registerRewardModuleType(String typeId, RewardModule.CallableRewardModule<RewardModule> callable) {
        ActivityRewarder.getInstance().registerModuleType(typeId, callable);
    }

    public static void unregisterRewardModuleType(String typeId) {
        ActivityRewarder.getInstance().unregisterModuleType(typeId);
    }
}
