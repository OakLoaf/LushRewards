package me.dave.activityrewarder.module.playtimegoals.playtimedailygoals;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaytimeDailyGoalsModule extends Module {
    private int refreshTime;
    private GuiFormat guiFormat;
    private HashMap<Integer, RewardCollection> minutesToReward;

    public PlaytimeDailyGoalsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getDailyGoalsConfig();

        // TODO: Fix below
        ConfigurationSection configurationSection = config.getConfigurationSection("daily-goals");
        if (configurationSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-goals' section in 'daily-playtime-goals.yml'");
            this.disable();
            return;
        }

        refreshTime = config.getInt("refresh-time");

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.minutesToReward = new HashMap<>();

        for (Map<?, ?> rewardMap : config.getMapList("daily-goals")) {
            RewardCollection rewardCollection;
            try {
                rewardCollection = RewardCollection.from(rewardMap);
            } catch(InvalidRewardException e) {
                e.printStackTrace();
                continue;
            }

            int minutes = rewardMap.containsKey("play-minutes") ? (int) rewardMap.get("play-minutes") * 60 : 60;
            minutesToReward.put(minutes, rewardCollection);
        }

        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from '" + configurationSection.getCurrentPath() + "'");
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
}
