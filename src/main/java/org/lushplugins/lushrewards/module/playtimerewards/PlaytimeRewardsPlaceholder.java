package org.lushplugins.lushrewards.module.playtimerewards;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.utils.placeholder.Placeholder;
import org.lushplugins.lushrewards.utils.placeholder.SimplePlaceholder;
import org.lushplugins.lushrewards.utils.placeholder.TimePlaceholder;

import java.util.HashSet;
import java.util.Optional;

public class PlaytimeRewardsPlaceholder {
    private static final HashSet<Placeholder> placeholderCache = new HashSet<>();

    static {
        placeholderCache.add(new TimePlaceholder("playtime", (params, player) -> {
            if (player == null || LushRewards.getInstance().getModule(params[0]).isEmpty()) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeRewardsModule module) {
                int globalPlaytime = rewardUser.getMinutesPlayed();
                if (module.getResetPlaytimeAt() <= 0) {
                    return globalPlaytime * 60;
                } else {
                    PlaytimeRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                    return (globalPlaytime - userData.getPreviousDayEndPlaytime()) * 60;
                }
            } else {
                return null;
            }
        }));

        placeholderCache.add(new TimePlaceholder("playtime_since_last_collected", (params, player) -> {
            if (player == null || LushRewards.getInstance().getModule(params[0]).isEmpty()) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeRewardsModule module) {
                PlaytimeRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return (rewardUser.getMinutesPlayed() - userData.getLastCollectedPlaytime()) * 60;
            } else {
                return null;
            }
        }));
    }

    private final String id;

    public PlaytimeRewardsPlaceholder(String id) {
        this.id = id;
    }

    public void register() {
        SimplePlaceholder modulePlaceholder = new SimplePlaceholder(id, (params, player) -> null);
        placeholderCache.forEach(modulePlaceholder::addChild);
        LushRewards.getInstance().getLocalPlaceholders().registerPlaceholder(modulePlaceholder);
    }

    public void unregister() {
        LushRewards.getInstance().getLocalPlaceholders().unregisterPlaceholder(id);
    }
}
