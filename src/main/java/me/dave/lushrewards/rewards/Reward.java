package me.dave.lushrewards.rewards;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.utils.SchedulerType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Reward implements Cloneable {
    private final String message;
    private final String broadcast;

    public Reward() {
        this.message = null;
        this.broadcast = null;
    }

    public Reward(@Nullable String message, @Nullable String broadcast) {
        this.message = message;
        this.broadcast = broadcast;
    }

    public Reward(Map<?, ?> map) {
        this.message = map.containsKey("message") ? (String) map.get("message") : null;
        this.broadcast = map.containsKey("broadcast") ? (String) map.get("broadcast") : null;
    }

    protected abstract void giveTo(Player player);

    protected abstract SchedulerType getSchedulerType();

    public abstract Map<String, Object> asMap();

    public void giveReward(Player player) {
        switch (this.getSchedulerType()) {
            case ASYNC -> LushRewards.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case PLAYER -> LushRewards.getMorePaperLib().scheduling().entitySpecificScheduler(player).run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            }, () -> {});
            case GLOBAL -> LushRewards.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
            case REGION -> LushRewards.getMorePaperLib().scheduling().regionSpecificScheduler(player.getLocation()).run(() -> {
                try {
                    this.giveTo(player);
                } catch (Exception e) {
                    LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" + this + ") to " + player.getName());
                    e.printStackTrace();
                }
            });
        }

        LushRewards.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            if (message != null) {
                ChatColorHandler.sendMessage(player, message.replaceAll("%player%", player.getDisplayName()));
            }
            if (broadcast != null) {
                ChatColorHandler.broadcastMessage(broadcast.replaceAll("%player%", player.getDisplayName()));
            }
        });
    }

    public static Reward loadReward(ConfigurationSection configurationSection) {
        return loadReward(configurationSection.getValues(false), configurationSection.getCurrentPath());
    }

    @Nullable
    public static Reward loadReward(Map<?, ?> rewardMap, String path) {
        Optional<RewardManager> optionalManager = LushRewards.getInstance().getManager(RewardManager.class);
        if (optionalManager.isEmpty()) {
            return null;
        }
        RewardManager rewardManager = optionalManager.get();

        String rewardType = (String) rewardMap.get("type");
        if (rewardType.equalsIgnoreCase("template")) {
            String template = (String) rewardMap.get("template");
            return LushRewards.getInstance().getConfigManager().getRewardTemplate(template);
        }

        if (!rewardManager.isRegistered(rewardType)) {
            LushRewards.getInstance().getLogger().severe("Invalid reward type at '" + path + "'");
            return null;
        }

        try {
            Constructor rewardConstructor = rewardManager.getConstructor(rewardType);
            return rewardConstructor != null ? rewardConstructor.build(rewardMap) : null;
        } catch (InvalidRewardException e) {
            LushRewards.getInstance().getLogger().warning(e.getCause().getMessage());
            return null;
        }
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

    @Override
    public Reward clone() {
        try {
            return (Reward) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @FunctionalInterface
    public interface Constructor {
        Reward build(Map<?, ?> map);
    }
}
