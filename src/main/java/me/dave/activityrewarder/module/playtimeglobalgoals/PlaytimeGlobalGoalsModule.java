package me.dave.activityrewarder.module.playtimeglobalgoals;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeGlobalGoalsModule extends Module {
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private ConcurrentHashMap<Integer, RewardCollection> minutesToReward;

    public PlaytimeGlobalGoalsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getGlobalGoalsConfig();

        if (!config.contains("global-goals")) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'global-goals' section in 'global-playtime-goals.yml'");
            this.disable();
            return;
        }

        refreshTime = config.getInt("refresh-time");
        receiveWithDailyRewards = config.getBoolean("give-with-daily-rewards");

        String guiTitle = config.getString("gui.title", "&8&lPlaytime Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.minutesToReward = new ConcurrentHashMap<>();
        for (Map<?, ?> rewardMap : config.getMapList("global-goals")) {
            RewardCollection rewardCollection;
            try {
                rewardCollection = RewardCollection.from(rewardMap);
            } catch(InvalidRewardException e) {
                e.printStackTrace();
                continue;
            }

            int minutes = rewardMap.containsKey("play-minutes") ? (int) rewardMap.get("play-minutes") : 60;
            minutesToReward.put(minutes, rewardCollection);
        }

        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from 'global-goals'");
    }

    @Override
    public void onDisable() {
        if (minutesToReward != null) {
            minutesToReward.clear();
            minutesToReward = null;
        }
    }

    public int getRefreshTime() {
        return refreshTime;
    }

    public boolean shouldReceiveWithDailyRewards() {
        return receiveWithDailyRewards;
    }

    @Nullable
    public RewardCollection getRewardCollection(int minutes) {
        return minutesToReward.get(minutes);
    }

    @NotNull
    public List<RewardCollection> getRewardCollectionsInRange(int lower, int upper) {
        return getKeysInRange(lower, upper).stream().map(key -> minutesToReward.get(key)).toList();
    }

    /**
     *
     * @param lower Lower bound (inclusive)
     * @param upper Upper bound (exclusive)
     * @return List of keys that fit within the range
     */
    @NotNull
    private List<Integer> getKeysInRange(int lower, int upper) {
        return minutesToReward.keySet().stream().filter(key -> key > lower && key <= upper).toList();
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    public boolean claimRewards(Player player) {
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
        PlaytimeGoalsModuleUserData playtimeGlobalGoalsModuleUserData = (PlaytimeGoalsModuleUserData) rewardUser.getModuleData(Module.ModuleType.GLOBAL_PLAYTIME_GOALS.getName());
        int minutesPlayed = rewardUser.getMinutesPlayed();

        List<RewardCollection> rewards = getRewardCollectionsInRange(playtimeGlobalGoalsModuleUserData.getLastCollectedPlaytime(), minutesPlayed);
        if (rewards.isEmpty()) {
            return false;
        }

        rewards.forEach(rewardCollection -> rewardCollection.giveAll(player));
        playtimeGlobalGoalsModuleUserData.setLastCollectedPlaytime(minutesPlayed);
        ActivityRewarder.getDataManager().saveRewardUser(rewardUser);

        return true;
    }
}
