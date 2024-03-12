package me.dave.lushrewards.module.playtimeglobalgoals;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.lushrewards.module.playtimegoals.PlaytimeGoalsModule;
import me.dave.lushrewards.rewards.collections.PlaytimeRewardCollection;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Combine playtime modules into one playtime module
public class PlaytimeGlobalGoalsModule extends RewardModule {
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;

    public PlaytimeGlobalGoalsModule(String id, File moduleFile) {
        super(id, moduleFile);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(moduleFile);
        if (!config.getBoolean("enabled", true)) {
            LushRewards.getInstance().getLogger().info("Module '" + id + "' is disabled in it's configuration");
            this.disable();
            return;
        }

        if (!config.contains("global-goals")) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'global-goals' section in 'global-playtime-goals.yml'");
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

        LushRewards.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from 'global-goals'");
    }

    @Override
    public void onDisable() {
        if (minutesToReward != null) {
            minutesToReward.clear();
            minutesToReward = null;
        }
    }

    public boolean claimRewards(Player player) {
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        PlaytimeGoalsModule.UserData playtimeGlobalGoalsModuleUserData = rewardUser.getModuleData(id);
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
        ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("global-playtime-reward-given")
            .replaceAll("%minutes%", String.valueOf(minutesSinceLastCollected))
            .replaceAll("%hours%", String.valueOf((int) Math.floor(minutesSinceLastCollected / 60D))));

        playtimeGlobalGoalsModuleUserData.setLastCollectedPlaytime(minutesPlayed);
        LushRewards.getInstance().getDataManager().saveRewardUser(rewardUser);

        return true;
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

    /**
     * Get RewardCollections and the amount of rewards within the range
     * @param lower Lower bound (inclusive)
     * @param upper Upper bound (exclusive)
     * @return A map of RewardCollections and the amount of rewards
     */
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

    public static class UserData extends UserDataModule.UserData {
        private int lastCollectedPlaytime;

        public UserData(String id, int lastCollectedPlaytime) {
            super(id);
            this.lastCollectedPlaytime = lastCollectedPlaytime;
        }

        public int getLastCollectedPlaytime() {
            return lastCollectedPlaytime;
        }

        public void setLastCollectedPlaytime(int lastCollectedPlaytime) {
            this.lastCollectedPlaytime = lastCollectedPlaytime;
        }
    }
}
