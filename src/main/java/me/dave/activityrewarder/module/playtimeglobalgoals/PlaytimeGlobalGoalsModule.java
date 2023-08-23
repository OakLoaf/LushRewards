package me.dave.activityrewarder.module.playtimeglobalgoals;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PlaytimeGlobalGoalsModule extends Module {
    // TODO: implement refresh time
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private HashMap<Integer, RewardCollection> minutesToReward;

    public PlaytimeGlobalGoalsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getGlobalGoalsConfig();
        ConfigurationSection configurationSection = config.getConfigurationSection("global-goals");
        if (configurationSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'global-goals' section in 'playtime-rewards.yml'");
            this.disable();
            return;
        }

        receiveWithDailyRewards = config.getBoolean("receive-with-daily-rewards");

        String guiTitle = config.getString("gui.title", "&8&lPlaytime Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.minutesToReward = new HashMap<>();
        for (Map.Entry<String, Object> entry : configurationSection.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection rewardSection) {
                RewardCollection rewardCollection;
                try {
                    rewardCollection = DailyRewardCollection.from(rewardSection);
                } catch(InvalidRewardException e) {
                    e.printStackTrace();
                    continue;
                }

                int minutes = (int) Math.floor(rewardSection.getDouble("play-hours", 1.0) * 60);
                minutesToReward.put(minutes, rewardCollection);
            }
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

    public boolean shouldReceiveWithDailyRewards() {
        return receiveWithDailyRewards;
    }

    @Nullable
    public RewardCollection getRewardCollection(int minutes) {
        // TODO: Work out how to implement this in a way that allows checking claimed rewards
        return minutesToReward.get(minutes);
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }
}
