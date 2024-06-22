package org.lushplugins.lushrewards.module.playtimerewards;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.exceptions.InvalidRewardException;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.lushplugins.lushrewards.gui.GuiFormat;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.lushplugins.lushrewards.rewards.collections.RewardCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushlib.gui.inventory.Gui;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlaytimeRewardsModule extends RewardModule implements UserDataModule<PlaytimeRewardsModule.UserData>, GuiDisplayer {
    private final ConcurrentHashMap<UUID, UserData> userDataCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;
    private PlaytimeRewardsPlaceholder placeholder;
    private int resetPlaytimeAt;
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private GuiFormat guiFormat;
    private PlaytimeRewardsGui.ScrollType scrollType;

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
        setShouldNotify(config.getBoolean("enable-notifications", false));

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);
        this.scrollType = PlaytimeRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "FIXED").toUpperCase());

        ConfigurationSection itemTemplatesSection = config.getConfigurationSection("gui.item-templates");
        if (itemTemplatesSection != null) {
            reloadItemTemplates(itemTemplatesSection);
        }

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
        return hasClaimableRewards(player, null);
    }

    public boolean hasClaimableRewards(Player player, Integer globalPlaytime) {
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        UserData userData = getUserData(player.getUniqueId());
        if (rewardUser == null || userData == null) {
            return false;
        }

        if (resetPlaytimeAt > 0 && !userData.getStartDate().isAfter(LocalDate.now().minusDays(resetPlaytimeAt))) {
            userData.setStartDate(LocalDate.now());
            userData.setPreviousDayEndPlaytime(userData.getLastCollectedPlaytime());
            saveUserData(userData.getUniqueId(), userData);
        }

        globalPlaytime = globalPlaytime != null ? globalPlaytime : rewardUser.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        return !getRewardCollectionsInRange(userData.getLastCollectedPlaytime() - previousDayEnd, globalPlaytime - previousDayEnd).isEmpty();
    }

    @Override
    public boolean claimRewards(Player player) {
        return claimRewards(player, null);
    }

    public boolean claimRewards(Player player, Integer globalPlaytime) {
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

        globalPlaytime = globalPlaytime != null ? globalPlaytime : rewardUser.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        int playtime = globalPlaytime - previousDayEnd;
        int lastCollectedPlaytime = Math.min(userData.getLastCollectedPlaytime() - previousDayEnd, 0);
        int playtimeSinceLastCollected = playtime - lastCollectedPlaytime;
        HashMap<PlaytimeRewardCollection, Integer> rewards = getRewardCollectionsInRange(lastCollectedPlaytime, playtime);
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

        String rewardMessage = LushRewards.getInstance().getConfigManager().getMessage("playtime-reward-given");
        if (rewardMessage.isBlank()) {
            // TODO: Deprecated for removal (Since 3.1.3)
            LushRewards.getInstance().getLogger().warning("Deprecated: The 'daily-playtime-reward-given' message has been renamed to 'playtime-reward-given', make sure to adjust the messages section of your config.");
            rewardMessage = LushRewards.getInstance().getConfigManager().getMessage("daily-playtime-reward-given");
        }

        ChatColorHandler.sendMessage(player, rewardMessage
            .replace("%minutes%", String.valueOf(playtimeSinceLastCollected))
            .replace("%hours%", String.valueOf((int) Math.floor(playtimeSinceLastCollected / 60D)))
            .replace("%total_minutes%", String.valueOf(playtime))
            .replace("%total_hours%", String.valueOf((int) Math.floor(playtime / 60D))));

        userData.setLastCollectedPlaytime(globalPlaytime);
        saveUserData(userData.getUniqueId(), userData);
        return true;
    }

    public int getResetPlaytimeAt() {
        return resetPlaytimeAt;
    }

    public int getRefreshTime() {
        return refreshTime;
    }

    public boolean shouldReceiveWithDailyRewards() {
        return receiveWithDailyRewards;
    }

    public Collection<PlaytimeRewardCollection> getRewards() {
        return minutesToReward.values();
    }

    @Nullable
    public RewardCollection getRewardCollection(int minutes) {
        return minutesToReward.get(minutes);
    }

    @NotNull
    public HashMap<PlaytimeRewardCollection, Integer> getRewardCollectionsInRange(int lower, int upper) {
        HashMap<PlaytimeRewardCollection, Integer> output = new HashMap<>();
        minutesToReward.values().forEach(rewardCollection -> {
            int amount = rewardCollection.amountAvailableAt(lower, upper);
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

    @Nullable
    public Integer getShortestRepeatFrequency() {
        return getShortestRepeatFrequency(-1);
    }

    /**
     * @param playtime Playtime to check for (-1 to ignore)
     * @return Shortest repeat frequency
     */
    @Nullable
    public Integer getShortestRepeatFrequency(int playtime) {
        Integer shortestFrequency = null;

        for (PlaytimeRewardCollection reward : minutesToReward.values()) {
            if (playtime >= 0 && reward.getRepeatsUntil() < playtime) {
                continue;
            }

            int frequency = reward.getRepeatFrequency();
            if (frequency > 0 && (shortestFrequency == null || frequency < shortestFrequency)) {
                shortestFrequency = frequency;
            }
        }

        return shortestFrequency;
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    @Override
    public Gui getGui(Player player) {
        return new PlaytimeRewardsGui(this, player);
    }

    public PlaytimeRewardsGui.ScrollType getScrollType() {
        return scrollType;
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
