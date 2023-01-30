package me.dave.activityrewarder.rewards;

import org.bukkit.entity.Player;

import java.util.ArrayList;

public class RewardsDay {
    private final String size;
    private final ArrayList<Reward> rewards;

    public RewardsDay(String size, ArrayList<Reward> rewards) {
        this.size = size;
        this.rewards = rewards;
    }

    public String getSize() {
        return size;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public void giveRewards(Player player) {
        for (Reward reward : rewards) {
            reward.giveReward(player);
        }
    }

    public void giveRewards(Player player, int multiplier) {
        for (int i = 0; i < multiplier; i++) {
            giveRewards(player);
        }
    }
}
