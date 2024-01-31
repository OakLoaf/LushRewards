package me.dave.activityrewarder.module.onetimerewards;

import me.dave.activityrewarder.module.RewardModule;
import org.bukkit.entity.Player;

import java.io.File;

public class OneTimeRewardsModule extends RewardModule {

    public OneTimeRewardsModule(String id, File moduleFile) {
        super(id, moduleFile);
    }

    @Override
    public boolean claimRewards(Player player) {
        return false;
    }
}
