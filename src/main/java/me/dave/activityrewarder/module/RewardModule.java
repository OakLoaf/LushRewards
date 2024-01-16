package me.dave.activityrewarder.module;

import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;

public abstract class RewardModule extends Module {

    public RewardModule(String id) {
        super(id);
    }

    abstract public boolean claimRewards(Player player);
}
