package org.lushplugins.lushrewards.module;

import org.lushplugins.lushrewards.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RewardModuleTypes {
    private static final Map<String, RewardModuleType<?>> types = new ConcurrentHashMap<>();

    public static final RewardModuleType<DailyRewardsModule> DAILY_REWARDS = register("daily-rewards", DailyRewardsModule::new);
    public static final RewardModuleType<PlaytimeRewardsModule> PLAYTIME_REWARDS = register("playtime-rewards", PlaytimeRewardsModule::new);

    public static boolean contains(String type) {
        return types.containsKey(type);
    }

    public static @Nullable RewardModuleType<?> get(String type) {
        return types.get(type);
    }

    public static <T extends RewardModule> RewardModuleType<T> register(String type, RewardModuleType.Constructor<T> constructor) {
        RewardModuleType<T> rewardModuleType = new RewardModuleType<>(type, constructor);
        types.put(type, rewardModuleType);
        return rewardModuleType;
    }

    public static void unregister(String type) {
        types.remove(type);
    }

    public static RewardModule constructModuleType(String type, String moduleId, File moduleFile) {
        return types.containsKey(type) ? types.get(type).build(moduleId, moduleFile) : null;
    }
}
