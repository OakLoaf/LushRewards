package org.lushplugins.lushrewards.module.dailyrewards;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.RewardDay;
import org.lushplugins.lushlib.module.Module;
import org.lushplugins.lushrewards.utils.placeholder.Placeholder;
import org.lushplugins.lushrewards.utils.placeholder.RegexPlaceholder;
import org.lushplugins.lushrewards.utils.placeholder.SimplePlaceholder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class DailyRewardsPlaceholder {
    private static final HashSet<Placeholder> placeholderCache = new HashSet<>();

    static {
        placeholderCache.add(new SimplePlaceholder("category", (params, player) -> {
            if (player == null) {
                return null;
            }
            UUID uuid = player.getUniqueId();

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(uuid);
                if (userData == null) {
                    return null;
                }

                RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreak());
                return String.valueOf(rewardDay.getHighestPriorityRewardCollection().getCategory());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new SimplePlaceholder("collected", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.hasCollectedToday());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new SimplePlaceholder("day_num", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.getDayNum());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new SimplePlaceholder("highest_streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.getHighestStreak());
            } else {
                return "0";
            }
        }));
        placeholderCache.add(new SimplePlaceholder("streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.getStreak());
            } else {
                return "0";
            }
        }));
        placeholderCache.add(new SimplePlaceholder("total_rewards", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreak());
                return String.valueOf(rewardDay.getRewardCount());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new RegexPlaceholder("day_[0-9]+.+", (params, player) -> {
            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());

                int dayNum = Integer.parseInt(params[2]);
                LocalDate date = userData.getExpectedDateOnDayNum(dayNum);
                RewardDay rewardDay = module.getRewardDay(date, dayNum);
                DailyRewardCollection dailyRewardCollection = rewardDay.getHighestPriorityRewardCollection();

                switch (params[3]) {
                    case "category" -> {
                        return String.valueOf(dailyRewardCollection.getCategory());
                    }
                    case "total_rewards" -> {
                        return String.valueOf(dailyRewardCollection.getRewardCount());
                    }
                }
            }

            return null;
        }));
    }

    private final String id;

    public DailyRewardsPlaceholder(String id) {
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
