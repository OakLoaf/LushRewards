package org.lushplugins.lushrewards.module.playtimerewards;

import org.lushplugins.lushlib.utils.Pair;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.utils.MathUtils;
import org.lushplugins.lushrewards.utils.placeholder.Placeholder;
import org.lushplugins.lushrewards.utils.placeholder.SimplePlaceholder;
import org.lushplugins.lushrewards.utils.placeholder.TimePlaceholder;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
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

        placeholderCache.add(new TimePlaceholder("time_since_reset", (params, player) -> {
            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof PlaytimeRewardsModule module) {
                int resetPlaytimeAt = module.getResetPlaytimeAt();
                if (resetPlaytimeAt > 0) {
                    PlaytimeRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                    if (userData != null) {
                        long start = LocalDateTime.of(userData.getStartDate(), LocalTime.MIN).toEpochSecond(ZoneOffset.UTC);
                        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
                        return (int) (now - start);
                    }
                }
            }

            return null;
        }));

        placeholderCache.add(new TimePlaceholder("time_until_next_reward", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isEmpty()) {
                return null;
            }

            if (!(optionalModule.get() instanceof PlaytimeRewardsModule module)) {
                return null;
            }

            RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser == null) {
                return null;
            }

            PlaytimeRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
            if (userData == null) {
                return null;
            }

            int startPlaytime = userData.getLastCollectedPlaytime();
            Integer nextRewardMinute = null;
            for (PlaytimeRewardCollection reward : module.getRewards()) {
                Integer minutes = MathUtils.findFirstNumInSequence(reward.getStartMinute(), reward.getRepeatFrequency(), startPlaytime);
                if (minutes != null) {
                    if (nextRewardMinute == null || minutes < nextRewardMinute) {
                        nextRewardMinute = minutes;
                    }
                }
            }

            if (nextRewardMinute == null) {
                return 0;
            }

            int remainingMinutes = Math.max(nextRewardMinute - rewardUser.getMinutesPlayed(), 0);
            return remainingMinutes * 60;
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
