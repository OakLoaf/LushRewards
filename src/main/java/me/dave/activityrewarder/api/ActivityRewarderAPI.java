package me.dave.activityrewarder.api;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.module.RewardModule;

@SuppressWarnings("unused")
public class ActivityRewarderAPI {

    public static void registerRewardModuleType(String typeId, RewardModule.Constructor<? extends RewardModule<RewardModule.UserData>> constructor) {
        ActivityRewarder.getInstance().registerModuleType(typeId, constructor);
    }

    public static void unregisterRewardModuleType(String typeId) {
        ActivityRewarder.getInstance().unregisterModuleType(typeId);
    }
}
