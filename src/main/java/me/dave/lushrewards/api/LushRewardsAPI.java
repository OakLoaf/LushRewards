package me.dave.lushrewards.api;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModuleTypeManager;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.rewards.RewardManager;
import me.dave.lushrewards.rewards.Reward;

@SuppressWarnings("unused")
public class LushRewardsAPI {

    public static void registerRewardModuleType(String id, RewardModule.Constructor<? extends RewardModule> constructor) {
        LushRewards.getInstance().getManager(RewardModuleTypeManager.class).ifPresent(manager -> manager.register(id, constructor));
    }

    public static void unregisterRewardModuleType(String id) {
        LushRewards.getInstance().getManager(RewardModuleTypeManager.class).ifPresent(manager -> manager.unregister(id));
    }

    public static void registerRewardType(String type, Reward.Constructor constructor) {
        LushRewards.getInstance().getManager(RewardManager.class).ifPresent(manager -> manager.register(type, constructor));
    }

    public static void unregisterRewardType(String type) {
        LushRewards.getInstance().getManager(RewardManager.class).ifPresent(manager -> manager.unregister(type));
    }
}
