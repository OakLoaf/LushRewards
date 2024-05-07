package org.lushplugins.lushrewards.api;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModuleTypeManager;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.rewards.RewardManager;
import org.lushplugins.lushrewards.rewards.Reward;

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
