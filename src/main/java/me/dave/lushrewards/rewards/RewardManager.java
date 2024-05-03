package me.dave.lushrewards.rewards;

import me.dave.lushrewards.rewards.custom.*;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.manager.Manager;

import java.util.concurrent.ConcurrentHashMap;

public class RewardManager extends Manager {
    private ConcurrentHashMap<String, Reward.Constructor> rewardTypes;

    @Override
    public void onEnable() {
        rewardTypes = new ConcurrentHashMap<>();

        register(Type.BROADCAST, BroadcastReward::new);
        register(Type.CONSOLE_COMMAND, ConsoleCommandReward::new);
        register(Type.ITEM, ItemReward::new);
        register(Type.MESSAGE, MessageReward::new);
        register(Type.PERMISSION, PermissionReward::new);
        register(Type.PLAYER_COMMAND, PlayerCommandReward::new);
        register(Type.RANDOM, RandomReward::new);
    }

    @Override
    public void onDisable() {
        if (rewardTypes != null) {
            rewardTypes.clear();
            rewardTypes = null;
        }
    }

    public boolean isRegistered(String type) {
        return rewardTypes.containsKey(type);
    }

    public void register(String type, Reward.Constructor constructor) {
        rewardTypes.put(type, constructor);
    }

    public void unregister(String type) {
        rewardTypes.remove(type);
    }

    @Nullable
    public Reward.Constructor getConstructor(String type) {
        return rewardTypes.get(type);
    }

    public static class Type {
        public static final String BROADCAST = "broadcast";
        public static final String CONSOLE_COMMAND = "command";
        public static final String ITEM = "item";
        public static final String MESSAGE = "message";
        public static final String PERMISSION = "permission";
        public static final String PLAYER_COMMAND = "player-command";
        public static final String RANDOM = "random";
    }
}
