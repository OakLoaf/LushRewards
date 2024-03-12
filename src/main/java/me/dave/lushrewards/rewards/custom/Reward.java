package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.rewards.RewardTypes;
import me.dave.lushrewards.utils.SchedulerType;
import me.dave.platyutils.PlatyUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Reward {
    protected abstract void giveTo(Player player);

    protected abstract SchedulerType getSchedulerType();

    public abstract Map<String, Object> asMap();

    public void giveReward(Player player) {
        switch (this.getSchedulerType()) {
            case ASYNC -> PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case PLAYER -> PlatyUtils.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            }, () -> {});
            case GLOBAL -> PlatyUtils.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case REGION -> PlatyUtils.getMorePaperLib().scheduling().regionSpecificScheduler(player.getLocation()).run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
        }
    }

    @Nullable
    public static Reward loadReward(Map<?, ?> rewardMap, String path) {
        String rewardType = (String) rewardMap.get("type");
        if (!RewardTypes.isRewardRegistered(rewardType)) {
            LushRewards.getInstance().getLogger().severe("Invalid reward type at '" + path + "'");
            return null;
        }

        try {
            return RewardTypes.getClass(rewardType).getConstructor(Map.class).newInstance(rewardMap);
        } catch (InvalidRewardException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            if (e instanceof InvocationTargetException invocationTargetException && invocationTargetException.getCause() instanceof InvalidRewardException cause) {
                LushRewards.getInstance().getLogger().warning(cause.getMessage());
            } else {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Nullable
    public static List<Reward> loadRewards(List<Map<?, ?>> maps, String path) {
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