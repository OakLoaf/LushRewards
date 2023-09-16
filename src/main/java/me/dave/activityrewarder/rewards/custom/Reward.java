package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.rewards.RewardTypes;
import me.dave.activityrewarder.utils.SchedulerType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface Reward {
    void giveTo(Player player);

    Map<String, Object> asMap();

    SchedulerType getSchedulerType();

    static void giveReward(Reward reward, Player player) {
        switch (reward.getSchedulerType()) {
            case ASYNC -> ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
                try {
                    reward.giveTo(player);
                } catch (Exception e) {
                    ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" + reward + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case PLAYER -> ActivityRewarder.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(() -> {
                try {
                    reward.giveTo(player);
                } catch (Exception e) {
                    ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" + reward + ") to " + player.getName());
                    e.printStackTrace();
                }
            }, () -> {});
            case GLOBAL -> ActivityRewarder.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                try {
                    reward.giveTo(player);
                } catch (Exception e) {
                    ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" + reward + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case REGION -> ActivityRewarder.getMorePaperLib().scheduling().regionSpecificScheduler(player.getLocation()).run(() -> {
                try {
                    reward.giveTo(player);
                } catch (Exception e) {
                    ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" + reward + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
        }
    }

    @Nullable
    static Reward loadReward(Map<?, ?> rewardMap, String path) {
        String rewardType = (String) rewardMap.get("type");
        if (!RewardTypes.isRewardRegistered(rewardType)) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid reward type at '" + path + "'");
            return null;
        }

        try {
            return RewardTypes.getClass(rewardType).getConstructor(Map.class).newInstance(rewardMap);
        } catch (InvalidRewardException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    static List<Reward> loadRewards(List<Map<?, ?>> maps, String path) {
        List<Reward> rewardList = new ArrayList<>();

        maps.forEach((map) -> {
            Reward reward = loadReward(map, path);
            if (reward != null) {
                rewardList.add(reward);
            }
        });

        return !rewardList.isEmpty() ? rewardList : null;
    }
}
