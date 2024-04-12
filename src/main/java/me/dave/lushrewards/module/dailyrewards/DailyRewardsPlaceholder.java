package me.dave.lushrewards.module.dailyrewards;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardDay;
import me.dave.lushrewards.utils.LocalPlaceholders;
import me.dave.platyutils.module.Module;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class DailyRewardsPlaceholder {
    private static final HashSet<LocalPlaceholders.Placeholder> placeholderCache = new HashSet<>();

    static {
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("category", (params, player) -> {
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

                RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreakLength());
                return String.valueOf(rewardDay.getHighestPriorityRewardCollection().getCategory());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("collected", (params, player) -> {
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
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("day_num", (params, player) -> {
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
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("highest_streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.getHighestStreak());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("streak", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                return String.valueOf(userData.getStreakLength());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new LocalPlaceholders.SimplePlaceholder("total_rewards", (params, player) -> {
            if (player == null) {
                return null;
            }

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());
                RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreakLength());
                return String.valueOf(rewardDay.getRewardCount());
            } else {
                return null;
            }
        }));
        placeholderCache.add(new LocalPlaceholders.RegexPlaceholder("day_[0-9]+.+", (params, player) -> {
            Optional<Module> optionalModule = LushRewards.getInstance().getModule(params[0]);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule module) {
                DailyRewardsModule.UserData userData = module.getUserData(player.getUniqueId());

                int dayNum = Integer.parseInt(params[2]);
                LocalDate date = userData.getDateAtStreakLength(dayNum);
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
        LocalPlaceholders.SimplePlaceholder modulePlaceholder = new LocalPlaceholders.SimplePlaceholder(id, (params, player) -> null);
        placeholderCache.forEach(modulePlaceholder::addChild);
    }

    public void unregister() {
        LushRewards.getInstance().getLocalPlaceholders().unregisterPlaceholder(id);
    }
}
