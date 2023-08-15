package me.dave.activityrewarder.module.dailyrewards;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleDate;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DailyRewardsModule extends Module {
    private Multimap<Integer, DailyRewardCollection> dayToRewards;
    private Multimap<SimpleDate, DailyRewardCollection> dateToRewards;
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

        this.dayToRewards = HashMultimap.create();
        this.dateToRewards = HashMultimap.create();

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        DailyRewardCollection defaultReward = null;
        for (Map.Entry<String, Object> entry : configurationSection.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection rewardSection) {
                DailyRewardCollection dailyRewardCollection = loadDailyRewardCollection(rewardSection);
                if (rewardSection.getName().equalsIgnoreCase("default")) {
                    defaultReward = dailyRewardCollection;
                } else {
                    dayToRewards.put(Integer.parseInt(rewardSection.getName().replaceAll("\\D", "")), dailyRewardCollection);
                }
            }
        }

        DailyRewardCollection.setDefaultReward(defaultReward);
        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + (dayToRewards.size() + (defaultReward != null ? 1 : 0)) + " reward collections from '" + configurationSection.getCurrentPath() + "'");
    }

    @Override
    public void onDisable() {
        if (dayToRewards != null) {
            dayToRewards.clear();
            dayToRewards = null;
        }

        if (dateToRewards != null) {
            dateToRewards.clear();
            dateToRewards = null;
        }

        guiFormat = null;
    }

    @NotNull
    public RewardDay getRewards(int day) {
        if (dayToRewards.containsKey(day)) {
            return RewardDay.from(dayToRewards.get(day));
        } else {
            return RewardDay.from(DailyRewardCollection.getDefaultReward());
        }
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
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
            RewardDay rewardDay = getRewards(rewardDayNum);
            if (rewardDay.containsRewardFromCategory(category)) {
                nextRewardDay = rewardDayNum;
            }
        }

        // Returns -1 if no future rewards match the request
        return nextRewardDay;
    }

    @NotNull
    private DailyRewardCollection loadDailyRewardCollection(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        String category = rewardCollectionSection.getString("category", "small");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        SimpleItemStack itemStack = itemSection != null ? SimpleItemStack.from(itemSection) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = ConfigParser.getSound(rewardCollectionSection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP").toUpperCase());

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + "rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return rewardList != null ? DailyRewardCollection.from(rewardList, 0, category, itemStack, redeemSound) : DailyRewardCollection.empty();
    }
}
