package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.*;

public class DailyRewardsModule extends Module {
    private int rewardsIndex;
    private HashMap<Integer, DailyRewardCollection> rewards;
    private Multimap<Integer, Integer> dayToRewards;
    private int resetDaysAt;
    private boolean dateAsAmount;
    private DailyRewardsGui.ScrollType scrollType;
    private GuiFormat guiFormat;

    public DailyRewardsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getDailyRewardsConfig();
        ConfigurationSection configurationSection = config.getConfigurationSection("daily-rewards");
        if (configurationSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section in 'daily-rewards.yml'");
            this.disable();
            return;
        }

        this.resetDaysAt = config.getInt("reset-days-at", -1);

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        this.dateAsAmount = config.getBoolean("gui.date-as-amount", false);
        this.scrollType = DailyRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "MONTH").toUpperCase());
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.rewardsIndex = 0;
        this.rewards = new HashMap<>();
        this.dayToRewards = HashMultimap.create();
        for (Map.Entry<String, Object> entry : configurationSection.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection rewardSection) {
                DailyRewardCollection dailyRewardCollection;
                try {
                    dailyRewardCollection = DailyRewardCollection.from(rewardSection);
                } catch(InvalidRewardException e) {
                    e.printStackTrace();
                    continue;
                }

                int rewardId = registerRewardCollection(dailyRewardCollection);

                if (dailyRewardCollection.getRewardDayNum() != null) {
                    dayToRewards.put(dailyRewardCollection.getRewardDayNum(), rewardId);
                }
            }
        }

        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + dayToRewards.size() + " reward collections from '" + configurationSection.getCurrentPath() + "'");
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

        guiFormat = null;
        rewardsIndex = 0;
    }

    public int registerRewardCollection(DailyRewardCollection collection) {
        rewardsIndex++;
        rewards.put(rewardsIndex, collection);
        return rewardsIndex;
    }

    @NotNull
    public Collection<DailyRewardCollection> getDayNumRewards(int day) {
        return rewards.values().stream().filter(rewardCollection -> rewardCollection.isAvailableOn(day)).toList();
    }

    @NotNull
    public Collection<DailyRewardCollection> getDateRewards(LocalDate date) {
        return rewards.values().stream().filter(rewardCollection -> rewardCollection.isAvailableOn(date)).toList();
    }

    public RewardDay getRewardDay(LocalDate date, int streakDay) {
        RewardDay rewardDay = new RewardDay();
        rewardDay.addCollections(getDateRewards(date));
        rewardDay.addCollections(getDayNumRewards(streakDay));
        return rewardDay;
    }

    // TODO: Take into account date rewards too
    public int findNextRewardFromCategory(int day, String category) {
//        List<DailyRewardCollection> filteredCollections = rewards.values().stream().filter(rewardCollection -> rewardCollection.getCategory().equalsIgnoreCase(category)).toList();
//
//        for (DailyRewardCollection collection : filteredCollections) {
//            if (collection.getRewardDate() != null && collection.getRewardDate().isBefore()) {
//                return
//            }
//        }

        int nextRewardDay = -1;

        // Iterates through dayToRewards
        for (int rewardDayNum : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardDayNum <= day || (nextRewardDay != -1 && rewardDayNum > nextRewardDay)) {
                continue;
            }

            // Gets the category of the reward and compares to the request
            RewardDay rewardDay = RewardDay.from(getDayNumRewards(rewardDayNum));
            if (rewardDay.containsRewardFromCategory(category)) {
                nextRewardDay = rewardDayNum;
            }
        }

        // Returns -1 if no future rewards match the request
        return nextRewardDay;
    }

    public int getResetDay() {
        return resetDaysAt;
    }

    public boolean showDateAsAmount() {
        return dateAsAmount;
    }

    public DailyRewardsGui.ScrollType getScrollType() {
        return scrollType;
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }
}
