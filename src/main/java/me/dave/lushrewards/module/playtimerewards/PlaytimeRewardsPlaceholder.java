package me.dave.lushrewards.module.playtimerewards;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.utils.LocalPlaceholders;
import org.lushplugins.lushlib.module.Module;

import java.util.HashSet;
import java.util.Optional;

public class PlaytimeRewardsPlaceholder {
    private static final HashSet<LocalPlaceholders.Placeholder> placeholderCache = new HashSet<>();

    static {
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("playtime_since_last_collected", (params, player) -> {
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
                return String.valueOf(rewardUser.getMinutesPlayed() - userData.getLastCollectedPlaytime());
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
        LocalPlaceholders.SimplePlaceholder modulePlaceholder = new LocalPlaceholders.SimplePlaceholder(id, (params, player) -> null);
        placeholderCache.forEach(modulePlaceholder::addChild);
    }

    public void unregister() {
        LushRewards.getInstance().getLocalPlaceholders().unregisterPlaceholder(id);
    }
}
