package me.dave.activityrewarder.rewards;

import org.bukkit.entity.Player;

import java.util.ArrayList;

public class RewardsDay {
    private final String size;
    private double multiplier = 1;
    private final ArrayList<Reward> rewards;

    public RewardsDay(String size, ArrayList<Reward> rewards) {
        this.size = size;
        this.rewards = rewards;
    }

    public String getSize() {
        return size;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void giveRewards(Player player) {
        for (Reward reward : rewards) {
            reward.giveReward(player);
        }
    }
}
