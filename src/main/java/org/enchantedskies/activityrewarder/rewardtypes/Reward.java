package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.entity.Player;

public abstract class Reward {
    public void giveReward(Player player) {
        giveReward(player, 0);
    }
    public void giveReward(Player player, int hourlyAmount) {}
}
