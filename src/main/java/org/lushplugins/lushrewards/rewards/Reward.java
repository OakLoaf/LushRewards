package org.lushplugins.lushrewards.rewards;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.exceptions.InvalidRewardException;
import org.lushplugins.lushrewards.utils.SchedulerType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Reward implements Cloneable {
    private String message = null;
    private String broadcast = null;

    public Reward() {}

    public Reward(@Nullable String message, @Nullable String broadcast) {
        this.message = message;
        this.broadcast = broadcast;
    }

    public Reward(Map<?, ?> map) {
        if (map.containsKey("message")) {
            this.message = (String) map.get("message");
        }
        if (map.containsKey("broadcast")) {
            this.broadcast = (String) map.get("broadcast");
        }
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
                ChatColorHandler.sendMessage(player, message.replace("%player%", player.getDisplayName()));
            }
            if (broadcast != null) {
                ChatColorHandler.broadcastMessage(broadcast.replace("%player%", player.getDisplayName()));
            }
        });
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
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
