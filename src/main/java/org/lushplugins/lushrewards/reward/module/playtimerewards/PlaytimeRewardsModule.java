package org.lushplugins.lushrewards.reward.module.playtimerewards;

import org.bukkit.Bukkit;
import org.lushplugins.guihandler.gui.Gui;
import org.lushplugins.guihandler.gui.GuiLayer;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.StoresUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.exception.InvalidRewardException;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.reward.RewardCollection;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.lushrewards.utils.GuiTemplates;
import revxrsal.commands.orphan.Orphans;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@StoresUserData(PlaytimeRewardsUserData.class)
public class PlaytimeRewardsModule extends RewardModule implements GuiDisplayer {
    private ConcurrentHashMap<Integer, PlaytimeRewardCollection> minutesToReward;
    private int resetPlaytimeAt;
    private int refreshTime;
    private boolean receiveWithDailyRewards;
    private Gui.Builder gui;

    public PlaytimeRewardsModule(String id, ConfigurationSection config) {
        super(id, config);

        // Finds relevant goals section - provides backwards compatibility
        String goalsSection = config.contains("goals") ? "goals" : config.contains("daily-goals") ? "daily-goals" : config.contains("global-goals") ? "global-goals" : null;
        if (goalsSection == null) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'goals' section in '" + id + "'");
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

        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiLayer guiLayer = templateType.equals("CUSTOM") ? new GuiLayer(config.getStringList("gui.format")) : GuiTemplates.valueOf(templateType);
        PlaytimeRewardsGui.ScrollType scrollType = PlaytimeRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "FIXED").toUpperCase());
        this.gui = LushRewards.getInstance().getGuiHandler().prepare(new PlaytimeRewardsGui(this, scrollType))
            .title(config.getString("gui.title", "&8&lPlaytime Rewards"))
            .size(guiLayer.getSize())
            .locked(true)
            .applyLayer(guiLayer);

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

        LushRewards.getInstance().getLogger().info("Successfully loaded " + minutesToReward.size() + " reward collections from 'goals'");

        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (!onlinePlayers.isEmpty()) {
            for (RewardUser user : LushRewards.getInstance().getUserCache().getCachedUsers()) {
                user.getModuleData(id, PlaytimeRewardsUserData.class);
            }

            LushRewards.getInstance().getLogger().info("Successfully loaded '" + id + "' user data for online players");
        }
    }

    @Override
    public void onStartup() {
        LushRewards.getInstance().getLamp().register(new Orphans(List.of("rewards module %s".formatted(this.id)))
            .handler(new PlaytimeRewardsCommands(this.id)));
    }

    public void checkForReset(RewardUser rewardUser, PlaytimeRewardsUserData userData) {
        if (resetPlaytimeAt <= 0) {
            return;
        }

        if (!userData.getStartDate().isAfter(LocalDate.now().minusDays(resetPlaytimeAt))) {
            Debugger.sendDebugMessage(String.format("Set start date for %s from %s to %s (Module: %s)", userData.getUniqueId(), userData.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), this.getId()), Debugger.DebugMode.PLAYTIME);
            userData.setStartDate(LocalDate.now());
            Debugger.sendDebugMessage(String.format("Set previous day end playtime for %s from %s to %s (Module: %s)", userData.getUniqueId(), userData.getPreviousDayEndPlaytime(), rewardUser.getMinutesPlayed(), this.getId()), Debugger.DebugMode.PLAYTIME);
            userData.setPreviousDayEndPlaytime(rewardUser.getMinutesPlayed());
            Debugger.sendDebugMessage(String.format("Set last collected playtime for %s from %s to %s (Module: %s)", userData.getUniqueId(), userData.getLastCollectedPlaytime(), rewardUser.getMinutesPlayed(), this.getId()), Debugger.DebugMode.PLAYTIME);
            userData.setLastCollectedPlaytime(rewardUser.getMinutesPlayed());

            LushRewards.getInstance().getStorageManager().saveModuleUserData(userData);
        }
    }

    public void checkAllOnlineForReset() {
        if (resetPlaytimeAt <= 0) {
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
            if (user == null) {
                continue;
            }

            PlaytimeRewardsUserData userData = user.getCachedModuleData(this.id, PlaytimeRewardsUserData.class);
            if (userData == null) {
                continue;
            }

            checkForReset(user, userData);
        }
    }

    public boolean hasClaimableRewardsAt(RewardUser user, Integer globalPlaytime) {
        PlaytimeRewardsUserData userData = user.getCachedModuleData(this.id, PlaytimeRewardsUserData.class);

        checkForReset(user, userData);

        globalPlaytime = globalPlaytime != null ? globalPlaytime : user.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        return !getRewardCollectionsInRange(userData.getLastCollectedPlaytime() - previousDayEnd, globalPlaytime - previousDayEnd).isEmpty();
    }

    @Override
    public boolean hasClaimableRewards(Player player, RewardUser user) {
        return hasClaimableRewardsAt(user, null);
    }

    public boolean claimRewards(Player player, RewardUser user, Integer globalPlaytime) {
        PlaytimeRewardsUserData userData = user.getCachedModuleData(this.id, PlaytimeRewardsUserData.class);

        boolean saveUserData = false;
        if (resetPlaytimeAt > 0 && userData.getStartDate().isEqual(LocalDate.now().minusDays(resetPlaytimeAt))) {
            Debugger.sendDebugMessage(String.format("Set start date for %s from %s to %s (Module: %s)", userData.getUniqueId(), userData.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE), LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), this.getId()), Debugger.DebugMode.PLAYTIME);
            userData.setStartDate(LocalDate.now());
            Debugger.sendDebugMessage(String.format("Set previous day end playtime for %s from %s to %s (Module: %s)", userData.getUniqueId(), userData.getPreviousDayEndPlaytime(), userData.getLastCollectedPlaytime(), this.getId()), Debugger.DebugMode.PLAYTIME);
            userData.setPreviousDayEndPlaytime(userData.getLastCollectedPlaytime());
            saveUserData = true;
        }

        globalPlaytime = globalPlaytime != null ? globalPlaytime : user.getMinutesPlayed();
        int previousDayEnd = userData.getPreviousDayEndPlaytime();
        int playtime = globalPlaytime - previousDayEnd;
        int lastCollectedPlaytime = Math.max(userData.getLastCollectedPlaytime() - previousDayEnd, 0);
        int playtimeSinceLastCollected = playtime - lastCollectedPlaytime;
        HashMap<PlaytimeRewardCollection, Integer> rewards = getRewardCollectionsInRange(lastCollectedPlaytime, playtime);
        if (rewards.isEmpty()) {
            if (saveUserData) {
                LushRewards.getInstance().getStorageManager().saveModuleUserData(userData);
            }

            return false;
        }

        rewards.forEach((rewardCollection, amount) -> {
            for (int i = 0; i < amount; i++) {
                rewardCollection.giveAll(player);
            }
        });

        ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("playtime-reward-given")
            .replace("%minutes%", String.valueOf(playtimeSinceLastCollected))
            .replace("%hours%", String.valueOf((int) Math.floor(playtimeSinceLastCollected / 60D)))
            .replace("%total_minutes%", String.valueOf(playtime))
            .replace("%total_hours%", String.valueOf((int) Math.floor(playtime / 60D))));

        userData.setLastCollectedPlaytime(globalPlaytime);
        LushRewards.getInstance().getStorageManager().saveModuleUserData(userData);
        return true;
    }

    @Override
    public boolean claimRewards(Player player, RewardUser user) {
        return claimRewards(player, user, null);
    }

    @Override
    public boolean requiresPlaytimeTracker() {
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

    @Override
    public Gui.Builder getGui() {
        return gui;
    }
}
