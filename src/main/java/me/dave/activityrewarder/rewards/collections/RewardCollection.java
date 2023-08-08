package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.rewards.custom.Reward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class RewardCollection {
    private final Collection<Reward> rewards;

    public RewardCollection(@Nullable Collection<Reward> rewards) {
        this.rewards = rewards != null ? rewards : Collections.emptyList();
    }

    public Collection<Reward> getRewards() {
        return rewards;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public boolean isEmpty() {
        return rewards.isEmpty();
    }

    public void giveAll(Player player) {
        rewards.forEach(reward -> reward.giveTo(player));
    }
}
