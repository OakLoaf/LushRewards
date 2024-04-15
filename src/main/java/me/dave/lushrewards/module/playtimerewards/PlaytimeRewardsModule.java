package me.dave.lushrewards.module.playtimerewards;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.lushrewards.rewards.collections.PlaytimeRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardCollection;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeRewardsModule extends RewardModule implements UserDataModule<PlaytimeRewardsModule.UserData> {
    private final ConcurrentHashMap<UUID, UserData> userDataCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;
    private PlaytimeRewardsPlaceholder placeholder;
    private int resetPlaytimeAt;
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;

    public PlaytimeRewardsModule(String id, File moduleFile) {
        super(id, moduleFile, true);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(moduleFile);
        if (!config.getBoolean("enabled", true)) {
            LushRewards.getInstance().getLogger().info("Module '" + id + "' is disabled in it's configuration");
            this.disable();
            return;
        }

        // Finds relevant goals section - provides backwards compatibility
        String goalsSection = config.contains("goals") ? "goals" : config.contains("daily-goals") ? "daily-goals" : config.contains("global-goals") ? "global-goals" : null;
        if (goalsSection == null) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'goals' section in '" + moduleFile.getName() + "'");
            this.disable();
            return;
        }

        if (config.contains("reset-playtime-at")) {
            resetPlaytimeAt = config.getInt("reset-playtime-at");
        } else if (id.contains("daily")) {
            resetPlaytimeAt = 1;
        } else if (id.contains("weekly")) {
            resetPlaytimeAt = 7;
        }

        refreshTime = config.getInt("refresh-time");
        receiveWithDailyRewards = config.getBoolean("give-with-daily-rewards");

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.minutesToReward = new ConcurrentHashMap<>();
        for (Map<?, ?> rewardMap : config.getMapList(goalsSection)) {
            PlaytimeRewardCollection rewardCollection;
            try {
                rewardCollection = PlaytimeRewardCollection.from(rewardMap);
            } catch (InvalidRewardException e) {
                e.printStackTrace();
                continue;
            }

            int minutes = rewardMap.containsKey("play-minutes") ? (int) rewardMap.get("play-minutes") : 60;
            minutesToReward.put(minutes, rewardCollection);
        }

        placeholder = new PlaytimeRewardsPlaceholder(id);
        placeholder.register();

        LushRewards.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from 'goals'");
    }

    @Override
    public void onDisable() {
        if (minutesToReward != null) {
            minutesToReward.clear();
            minutesToReward = null;
        }

        if (placeholder != null) {
            placeholder.unregister();
            placeholder = null;
        }

        userDataCache.clear();
    }

    @Override
    public boolean hasClaimableRewards(Player player) {
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        UserData userData = getUserData(player.getUniqueId());
        if (rewardUser == null || userData == null) {
            return false;
        }

        if (resetPlaytimeAt > 0 && userData.getStartDate().isEqual(LocalDate.now().minusDays(resetPlaytimeAt))) {
            userData.setStartDate(LocalDate.now());
            userData.setPreviousDayEndPlaytime(userData.getLastCollectedPlaytime());
            saveUserData(userData.getUniqueId(), userData);
        }

        int totalMinutesPlayed = rewardUser.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        return !getRewardCollectionsInRange(userData.getLastCollectedPlaytime() - previousDayEnd, totalMinutesPlayed - previousDayEnd).isEmpty();
    }

    @Override
    public boolean claimRewards(Player player) {
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        UserData userData = getUserData(player.getUniqueId());
        if (rewardUser == null || userData == null) {
            ChatColorHandler.sendMessage(player, "&#ff6969Failed to collect your reward user data, try relogging. If this continues inform an administrator");
            return false;
        }

        boolean saveUserData = false;
        if (resetPlaytimeAt > 0 && userData.getStartDate().isEqual(LocalDate.now().minusDays(resetPlaytimeAt))) {
            userData.setStartDate(LocalDate.now());
            userData.setPreviousDayEndPlaytime(userData.getLastCollectedPlaytime());
            saveUserData = true;
        }

        int totalMinutesPlayed = rewardUser.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        HashMap<PlaytimeRewardCollection, Integer> rewards = getRewardCollectionsInRange(userData.getLastCollectedPlaytime() - previousDayEnd, totalMinutesPlayed - previousDayEnd);
        if (rewards.isEmpty()) {
            if (saveUserData) {
                saveUserData(userData.getUniqueId(), userData);
            }
            return false;
        }

        rewards.forEach((rewardCollection, amount) -> {
            for (int i = 0; i < amount; i++) {
                rewardCollection.giveAll(player);
            }
        });

        ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("daily-playtime-reward-given")
            .replaceAll("%minutes%", String.valueOf(rewardUser.getMinutesPlayed()))
            .replaceAll("%hours%", String.valueOf((int) Math.floor(rewardUser.getMinutesPlayed() / 60D))));

        userData.setLastCollectedPlaytime(totalMinutesPlayed);
        rewardUser.save();
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
     * @param lower Lower bound (inclusive)
     * @param upper Upper bound (exclusive)
     * @return List of keys that fit within the range
     */
    @NotNull
    private List<Integer> getKeysInRange(int lower, int upper) {
        return minutesToReward.keySet().stream().filter(key -> key > lower && key <= upper).toList();
    }

    // TODO: Add Gui
    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    @Override
    public UserData getDefaultData(UUID uuid) {
        return new UserData(uuid, id, 0, LocalDate.now(), 0);
    }

    @Override
    public UserData getUserData(UUID uuid) {
        return userDataCache.get(uuid);
    }

    @Override
    public void cacheUserData(UUID uuid, UserDataModule.UserData userData) {
        if (userData instanceof UserData data) {
            userDataCache.put(uuid, data);
        }
    }

    @Override
    public void uncacheUserData(UUID uuid) {
        userDataCache.remove(uuid);
    }

    @Override
    public Class<UserData> getUserDataClass() {
        return UserData.class;
    }

    public static class UserData extends UserDataModule.UserData {
        private int lastCollectedPlaytime;
        private LocalDate startDate;
        private int previousDayEndPlaytime;

        public UserData(UUID uuid, String id, int lastCollectedPlaytime, @NotNull LocalDate startDate, int previousDayEndPlaytime) {
            super(uuid, id);
            this.lastCollectedPlaytime = lastCollectedPlaytime;
            this.startDate = startDate;
            this.previousDayEndPlaytime = previousDayEndPlaytime;
        }

        public int getLastCollectedPlaytime() {
            return lastCollectedPlaytime;
        }

        public void setLastCollectedPlaytime(int lastCollectedPlaytime) {
            this.lastCollectedPlaytime = lastCollectedPlaytime;
        }

        @NotNull
        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(@NotNull LocalDate startDate) {
            this.startDate = startDate;
        }

        public int getPreviousDayEndPlaytime() {
            return previousDayEndPlaytime;
        }

        public void setPreviousDayEndPlaytime(int previousDayEndPlaytime) {
            this.previousDayEndPlaytime = previousDayEndPlaytime;
        }
    }
}
