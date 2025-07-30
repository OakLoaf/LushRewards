package org.lushplugins.lushrewards.placeholder;

import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardCollection;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsModule;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardsUserData;
import org.lushplugins.lushrewards.reward.RewardDay;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.placeholderhandler.annotation.Placeholder;
import org.lushplugins.placeholderhandler.annotation.SubPlaceholder;

import java.time.LocalDate;

@SuppressWarnings("unused")
@Placeholder("lushrewards")
public class DailyRewardsPlaceholders {

    @SubPlaceholder("<module>_category")
    public String category(RewardUser user, DailyRewardsModule module) {
        DailyRewardsUserData userData = user.getModuleData(module.getId(), DailyRewardsUserData.class);
        if (userData == null) {
            return null;
        }

        RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreak());
        return String.valueOf(rewardDay.getHighestPriorityRewardCollection().getCategory());
    }

    @SubPlaceholder("<moduleId>_collected")
    public String collected(RewardUser user, String moduleId) {
        DailyRewardsUserData userData = user.getModuleData(moduleId, DailyRewardsUserData.class);
        return userData != null ? String.valueOf(userData.hasCollectedToday()) : null;
    }

    @SubPlaceholder("<moduleId>_day_num")
    public String dayNum(RewardUser user, String moduleId) {
        DailyRewardsUserData userData = user.getModuleData(moduleId, DailyRewardsUserData.class);
        return userData != null ? String.valueOf(userData.getDayNum()) : null;
    }

    @SubPlaceholder("<moduleId>_streak")
    public String streak(RewardUser user, String moduleId) {
        DailyRewardsUserData userData = user.getModuleData(moduleId, DailyRewardsUserData.class);
        return userData != null ? String.valueOf(userData.getStreak()) : "0";
    }

    @SubPlaceholder("<moduleId>_highest_streak")
    public String highestStreak(RewardUser user, String moduleId) {
        DailyRewardsUserData userData = user.getModuleData(moduleId, DailyRewardsUserData.class);
        return userData != null ? String.valueOf(userData.getHighestStreak()) : "0";
    }

    @SubPlaceholder("<module>_total_rewards")
    public String totalRewards(RewardUser user, DailyRewardsModule module) {
        DailyRewardsUserData userData = user.getModuleData(module.getId(), DailyRewardsUserData.class);
        if (userData == null) {
            return null;
        }

        RewardDay rewardDay = module.getRewardDay(LocalDate.now(), userData.getStreak());
        return String.valueOf(rewardDay.getRewardCount());
    }

    private DailyRewardCollection getRewardCollection(RewardUser user, DailyRewardsModule module, int dayNum) {
        DailyRewardsUserData userData = user.getModuleData(module.getId(), DailyRewardsUserData.class);
        if (userData == null) {
            return null;
        }

        LocalDate date = userData.getExpectedDateOnDayNum(dayNum);
        RewardDay rewardDay = module.getRewardDay(date, dayNum);
        return rewardDay.getHighestPriorityRewardCollection();
    }

    @SubPlaceholder("<module>_day_<dayNum>_category")
    public String dayNumCategory(RewardUser user, DailyRewardsModule module, int dayNum) {
        DailyRewardCollection rewardCollection = getRewardCollection(user, module, dayNum);
        return rewardCollection != null ? String.valueOf(rewardCollection.getCategory()) : null;
    }

    @SubPlaceholder("<module>_day_<dayNum>_total_rewards")
    public String dayNumTotalRewards(RewardUser user, DailyRewardsModule module, int dayNum) {
        DailyRewardCollection rewardCollection = getRewardCollection(user, module, dayNum);
        return rewardCollection != null ? String.valueOf(rewardCollection.getRewardCount()) : null;
    }
}
