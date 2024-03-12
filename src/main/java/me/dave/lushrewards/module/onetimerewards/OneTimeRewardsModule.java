package me.dave.lushrewards.module.onetimerewards;

import me.dave.lushrewards.module.RewardModule;
import org.bukkit.entity.Player;

import java.io.File;

public class OneTimeRewardsModule extends RewardModule {

    public OneTimeRewardsModule(String id, File moduleFile) {
        super(id, moduleFile);
    }

    @Override
    public boolean hasClaimableRewards(Player player) {
        return false;
    }

    @Override
    public boolean claimRewards(Player player) {
        return false;
    }
}
