package me.dave.lushrewards.module.playtimedailygoals;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.lushrewards.module.playtimeglobalgoals.PlaytimeGoalsModuleUserData;
import me.dave.lushrewards.rewards.collections.PlaytimeRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardCollection;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// TODO: Combine playtime modules into one playtime module
public class PlaytimeDailyGoalsModule extends RewardModule implements UserDataModule<PlaytimeGoalsModuleUserData> {
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;

    public PlaytimeDailyGoalsModule(String id, File moduleFile) {
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

        if (!config.contains("daily-goals")) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-goals' section in 'daily-playtime-goals.yml'");
            this.disable();
            return;
        }

        refreshTime = config.getInt("refresh-time");
        receiveWithDailyRewards = config.getBoolean("give-with-daily-rewards");

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.minutesToReward = new ConcurrentHashMap<>();
        for (Map<?, ?> rewardMap : config.getMapList("daily-goals")) {
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

        LushRewards.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from 'daily-goals'");
    }

    @Override
    public void onDisable() {
        if (minutesToReward != null) {
            minutesToReward.clear();
            minutesToReward = null;
        }
    }

    public boolean claimRewards(Player player) {
//        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        UserData playtimeDailyGoalsUserData = (UserData) rewardUser.getModuleData(id);
        int totalMinutesPlayed = rewardUser.getMinutesPlayed();

        boolean saveRewardUser = false;
        if (!playtimeDailyGoalsUserData.getDate().isEqual(LocalDate.now())) {
            playtimeDailyGoalsUserData.setDate(LocalDate.now());
            playtimeDailyGoalsUserData.setPreviousDayEndPlaytime(playtimeDailyGoalsUserData.getLastCollectedPlaytime());
            saveRewardUser = true;
        }

        int previousDayEnd =  playtimeDailyGoalsUserData.getPreviousDayEndPlaytime();
        HashMap<PlaytimeRewardCollection, Integer> rewards = getRewardCollectionsInRange(playtimeDailyGoalsUserData.getLastCollectedPlaytime() - previousDayEnd, totalMinutesPlayed - previousDayEnd);
        if (rewards.isEmpty()) {
            if (saveRewardUser) {
                LushRewards.getInstance().getDataManager().saveRewardUser(player);
            }
            return false;
        }

        rewards.forEach((rewardCollection, amount) -> {
            for (int i = 0; i < amount; i++) {
                rewardCollection.giveAll(player);
            }
        });

        ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("daily-playtime-reward-given")
            .replaceAll("%minutes%", String.valueOf(LushRewards.getInstance().getDataManager().getRewardUser(player).getMinutesPlayed()))
            .replaceAll("%hours%", String.valueOf((int) Math.floor(LushRewards.getInstance().getDataManager().getRewardUser(player).getMinutesPlayed() / 60D))));

        playtimeDailyGoalsUserData.setLastCollectedPlaytime(totalMinutesPlayed);
        LushRewards.getInstance().getDataManager().saveRewardUser(player);

        return true;
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

    @Override
    public Class<PlaytimeGoalsModuleUserData> getUserDataClass() {
        return PlaytimeGoalsModuleUserData.class;
    }

    @Override
    public PlaytimeGoalsModuleUserData getDefaultData() {
        return new PlaytimeGoalsModuleUserData(id, 0);
    }

    @Override
    public PlaytimeGoalsModuleUserData getUserData(UUID uuid) {
        return null;
    }

    @Override
    public void loadUserData(UUID uuid, UserDataModule.UserData userData) {

    }

    @Override
    public void unloadUserData(UUID uuid) {

    }

    public static class UserData extends PlaytimeGoalsModuleUserData {
        private LocalDate date;
        private int previousDayEndPlaytime;

        public UserData(String id, int lastCollectedPlaytime, @NotNull LocalDate date, int previousDayEndPlaytime) {
            super(id, lastCollectedPlaytime);
            this.date = date;
            this.previousDayEndPlaytime = previousDayEndPlaytime;
        }

        @NotNull
        public LocalDate getDate() {
            return date;
        }

        public void setDate(@NotNull LocalDate date) {
            this.date = date;
        }

        public int getPreviousDayEndPlaytime() {
            return previousDayEndPlaytime;
        }

        public void setPreviousDayEndPlaytime(int previousDayEndPlaytime) {
            this.previousDayEndPlaytime = previousDayEndPlaytime;
        }
    }
}
