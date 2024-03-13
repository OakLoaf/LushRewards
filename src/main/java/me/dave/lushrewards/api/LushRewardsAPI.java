package me.dave.lushrewards.api;

import me.dave.lushrewards.module.RewardModuleTypeManager;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.rewards.RewardTypeManager;
import me.dave.lushrewards.rewards.custom.Reward;
import me.dave.platyutils.PlatyUtils;

@SuppressWarnings("unused")
public class LushRewardsAPI {

    public static void registerRewardModuleType(String id, RewardModule.Constructor<? extends RewardModule> constructor) {
        PlatyUtils.getManager(RewardModuleTypeManager.class).ifPresent(manager -> manager.register(id, constructor));
    }

    public static void unregisterRewardModuleType(String id) {
        PlatyUtils.getManager(RewardModuleTypeManager.class).ifPresent(manager -> manager.unregister(id));
    }

    public static void registerRewardType(String type, Reward.Constructor constructor) {
        PlatyUtils.getManager(RewardTypeManager.class).ifPresent(manager -> manager.register(type, constructor));
    }

    public static void unregisterRewardType(String type) {
        PlatyUtils.getManager(RewardTypeManager.class).ifPresent(manager -> manager.unregister(type));
    }
}
