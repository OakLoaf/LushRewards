package me.dave.activityrewarder.module.playtimeglobalgoals;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.PlaytimeRewardCollection;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeGlobalGoalsModule extends Module {
    public static final String ID = "global-playtime-goals";
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;

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
            PlaytimeRewardCollection rewardCollection;
            try {
                rewardCollection = PlaytimeRewardCollection.from(rewardMap);
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
    public PlaytimeRewardCollection getRewardCollection(int minutes) {
        return minutesToReward.get(minutes);
    }

    @NotNull
    public HashMap<PlaytimeRewardCollection, Integer> getRewardCollectionsInRange(int lower, int upper) {
        HashMap<PlaytimeRewardCollection, Integer> output = new HashMap<>();
        minutesToReward.values().forEach(rewardCollection -> {
            int amount = rewardCollection.isAvailableAt(lower, upper);
            if (amount > 0) {
                output.put(rewardCollection, amount);
            }
        });
        return output;
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    public boolean claimRewards(Player player) {
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
        PlaytimeGoalsModuleUserData playtimeGlobalGoalsModuleUserData = (PlaytimeGoalsModuleUserData) rewardUser.getModuleData(PlaytimeGlobalGoalsModule.ID);
        int minutesPlayed = rewardUser.getMinutesPlayed();

        HashMap<PlaytimeRewardCollection, Integer> rewards = getRewardCollectionsInRange(playtimeGlobalGoalsModuleUserData.getLastCollectedPlaytime(), minutesPlayed);
        if (rewards.isEmpty()) {
            return false;
        }

        rewards.forEach((rewardCollection, amount) -> {
            for (int i = 0; i < amount; i++) {
                rewardCollection.giveAll(player);
            }
        });

        int minutesSinceLastCollected = rewardUser.getMinutesPlayed() - playtimeGlobalGoalsModuleUserData.getLastCollectedPlaytime();
        ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("global-playtime-reward-given")
            .replaceAll("%minutes%", String.valueOf(minutesSinceLastCollected))
            .replaceAll("%hours%", String.valueOf((int) Math.floor(minutesSinceLastCollected / 60D))));

        playtimeGlobalGoalsModuleUserData.setLastCollectedPlaytime(minutesPlayed);
        ActivityRewarder.getDataManager().saveRewardUser(rewardUser);

        return true;
    }
}
