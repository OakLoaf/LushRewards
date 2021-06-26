package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.entity.Player;

public interface Reward {
    String getSize();
    void giveReward(Player player);
}
