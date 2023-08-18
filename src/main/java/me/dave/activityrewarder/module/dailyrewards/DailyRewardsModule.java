package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.SimpleDate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DailyRewardsModule extends Module {
    private int rewardsIndex;
    private HashMap<Integer, DailyRewardCollection> rewards;
    private Multimap<Integer, Integer> dayToRewards;
    private Multimap<SimpleDate, Integer> dateToRewards;
    private GuiFormat guiFormat;

    public DailyRewardsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getDailyRewardsConfig();
        ConfigurationSection configurationSection = config.getConfigurationSection("daily-rewards");
        if (configurationSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section");
            this.disable();
            return;
        }

        this.rewardsIndex = 0;
        this.rewards = new HashMap<>();
        this.dayToRewards = HashMultimap.create();
        this.dateToRewards = HashMultimap.create();

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        DailyRewardCollection defaultReward = null;
        for (Map.Entry<String, Object> entry : configurationSection.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection rewardSection) {
                DailyRewardCollection dailyRewardCollection;
                try {
                    dailyRewardCollection = DailyRewardCollection.from(rewardSection);
                } catch(InvalidRewardException e) {
                    e.printStackTrace();
                    continue;
                }

                if (rewardSection.getName().equalsIgnoreCase("default")) {
                    defaultReward = dailyRewardCollection;
                } else {
                    int rewardId = registerRewardCollection(dailyRewardCollection);

                    if (dailyRewardCollection.getDate() != null) {
                        dateToRewards.put(dailyRewardCollection.getDate(), rewardId);
                    }
                    if (dailyRewardCollection.getStreakDay() != null) {
                        dayToRewards.put(dailyRewardCollection.getStreakDay(), rewardId);
                    }
                }
            }
        }

        DailyRewardCollection.setDefaultReward(defaultReward);
        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + (dayToRewards.size() + (defaultReward != null ? 1 : 0)) + " reward collections from '" + configurationSection.getCurrentPath() + "'");
    }

    @Override
    public void onDisable() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }

        if (dayToRewards != null) {
            dayToRewards.clear();
            dayToRewards = null;
        }

        if (dateToRewards != null) {
            dateToRewards.clear();
            dateToRewards = null;
        }

        guiFormat = null;
        rewardsIndex = 0;
    }

    public int registerRewardCollection(DailyRewardCollection collection) {
        rewardsIndex++;
        rewards.put(rewardsIndex, collection);
        return rewardsIndex;
    }

    @NotNull
    public Collection<DailyRewardCollection> getStreakRewards(int day) {
        if (dayToRewards.containsKey(day)) {
            return dayToRewards.get(day)
                .stream()
                .map(collectionId -> rewards.get(collectionId))
                .toList();
        } else {
            return new ArrayList<>();
        }
    }

    @NotNull
    public Collection<DailyRewardCollection> getDateRewards(SimpleDate date) {
        if (dateToRewards.containsKey(date)) {
            return dateToRewards.get(date)
                .stream()
                .map(collectionId -> rewards.get(collectionId))
                .toList();
        } else {
            return new ArrayList<>();
        }
    }

    public RewardDay getRewardDay(SimpleDate date, int streakDay) {
        RewardDay rewardDay = new RewardDay();
        rewardDay.addCollections(getDateRewards(date));
        rewardDay.addCollections(getStreakRewards(streakDay));

        if (rewardDay.isEmpty()) rewardDay.addCollection(DailyRewardCollection.getDefaultReward());

        return rewardDay;
    }

    public int findNextRewardFromCategory(int day, String category) {
        int nextRewardDay = -1;

        // Iterates through dayToRewards
        for (int rewardDayNum : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardDayNum <= day || (nextRewardDay != -1 && rewardDayNum > nextRewardDay)) {
                continue;
            }

            // Gets the category of the reward and compares to the request
            RewardDay rewardDay = RewardDay.from(getStreakRewards(rewardDayNum));
            if (rewardDay.containsRewardFromCategory(category)) {
                nextRewardDay = rewardDayNum;
            }
        }

        // Returns -1 if no future rewards match the request
        return nextRewardDay;
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }
}
