package org.lushplugins.lushrewards.reward.module.dailyrewards;

import org.bukkit.Registry;
import org.jetbrains.annotations.ApiStatus;
import org.lushplugins.guihandler.gui.Gui;
import org.lushplugins.guihandler.gui.GuiLayer;
import org.lushplugins.lushlib.registry.RegistryUtils;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.StoresUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.exception.InvalidRewardException;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.playtime.PlaytimeTrackerManager;
import org.lushplugins.lushrewards.reward.RewardDay;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.utils.GuiTemplates;
import revxrsal.commands.orphan.Orphans;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

@StoresUserData(DailyRewardsUserData.class)
public class DailyRewardsModule extends RewardModule implements GuiDisplayer {
    private HashSet<DailyRewardCollection> rewards;
    private int resetDaysAt;
    private RewardMode rewardMode;
    private boolean allowRewardsStacking;
    private boolean streakBypass;
    private Sound defaultRedeemSound;
    private String upcomingCategory;
    private String rewardPlaceholderClaimed;
    private String rewardPlaceholderUnclaimed;
    private Gui.Builder gui;
    private @ApiStatus.Internal Integer requiredPlaytime; // TODO: Properly implement conditions

    public DailyRewardsModule(String id, ConfigurationSection config) {
        super(id, config);

        ConfigurationSection rewardsSection = config.getConfigurationSection("daily-rewards");
        if (rewardsSection == null) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section in '" + id + "'");
            return;
        }

        this.resetDaysAt = config.getInt("reset-days-at", -1);
        this.rewardMode = StringUtils.getEnum(config.getString("reward-mode", config.getBoolean("streak-mode") ? "streak" : "default"), RewardMode.class).orElse(RewardMode.DEFAULT);
        this.allowRewardsStacking = config.getBoolean("allow-reward-stacking", true);
        this.streakBypass = config.getBoolean("streak-bypass");

        this.defaultRedeemSound = RegistryUtils.parseString(config.getString("default-redeem-sound", "none"), Registry.SOUNDS);
        setShouldNotify(config.getBoolean("enable-notifications", true));
        this.upcomingCategory = config.getString("upcoming-category");

        this.rewardPlaceholderClaimed = config.getString("reward-placeholders.claimed", "true");
        this.rewardPlaceholderUnclaimed = config.getString("reward-placeholders.unclaimed", "false");

        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiLayer guiLayer = templateType.equals("CUSTOM") ? new GuiLayer(config.getStringList("gui.format")) : GuiTemplates.valueOf(templateType);
        DailyRewardsGui.ScrollType scrollType = DailyRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "MONTH").toUpperCase());
        boolean showDateAsAmount = config.getBoolean("gui.date-as-amount", false);
        this.gui = LushRewards.getInstance().getGuiHandler().prepare(new DailyRewardsGui(this, scrollType, showDateAsAmount))
            .title(config.getString("gui.title", "&8&lDaily Rewards"))
            .size(guiLayer.getSize())
            .locked(true)
            .applyLayer(guiLayer);

        this.requiredPlaytime = config.contains("required-playtime") ? config.getInt("required-playtime") : null;

        ConfigurationSection itemTemplatesSection = config.getConfigurationSection("gui.item-templates");
        if (itemTemplatesSection != null) {
            reloadItemTemplates(itemTemplatesSection);
        }

        this.rewards = new HashSet<>();

        LocalDate today = LocalDate.now();
        for (Object entry : rewardsSection.getValues(false).values()) {
            if (entry instanceof ConfigurationSection rewardSection) {
                DailyRewardCollection dailyRewardCollection;
                try {
                    dailyRewardCollection = DailyRewardCollection.from(rewardSection);
                } catch (InvalidRewardException e) {
                    e.printStackTrace();
                    continue;
                }

                if (LushRewards.getInstance().getConfigManager().isPerformanceModeEnabled() && dailyRewardCollection.getRewardDate() != null) {
                    LocalDate lowestDate;
                    LocalDate highestDate;
                    switch (scrollType) {
                        case DAY -> {
                            lowestDate = LocalDate.now();
                            highestDate = today.plusDays(guiLayer.getCharCount('R') - 1);
                        }
                        case MONTH -> {
                            lowestDate = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
                            highestDate = LocalDate.of(today.getYear(), today.getMonthValue(), today.getMonth().length(today.isLeapYear()));
                        }
                        // Uses GRID mode by default as this has the largest range of possible dates
                        default -> {
                            int rewardDisplayCount = guiLayer.getCharCount('R');

                            lowestDate = today.minusDays(rewardDisplayCount - 1);
                            highestDate = today.plusDays(rewardDisplayCount - 1);
                        }
                    }

                    for (LocalDate dateIndex = lowestDate; !dateIndex.isAfter(highestDate); dateIndex = dateIndex.plusDays(1)) {
                        if (dailyRewardCollection.isAvailableOn(dateIndex)) {
                            rewards.add(dailyRewardCollection);
                            break;
                        }
                    }
                } else {
                    rewards.add(dailyRewardCollection);
                }
            }
        }

        LushRewards.getInstance().getLogger().info("Successfully loaded %s reward collections from '%s'"
            .formatted(rewards.size(), rewardsSection.getCurrentPath()));
    }

    @Override
    public void onStartup() {
        LushRewards.getInstance().getLamp().register(new Orphans(List.of("rewards module %s".formatted(this.id)))
            .handler(new DailyRewardsCommands(this.id)));
    }

    public boolean meetsRequiredPlaytime(Player player) {
        if (requiredPlaytime == null) {
            return true;
        }

        PlaytimeTrackerManager playtimeTrackerManager = LushRewards.getInstance().getPlaytimeTrackerManager();
        if (playtimeTrackerManager.isEnabled()) {
            return playtimeTrackerManager.getPlaytimeTracker(player.getUniqueId()).getSessionPlaytime() > requiredPlaytime;
        }

        return true;
    }

    @Override
    public boolean hasClaimableRewards(Player player, RewardUser user) {
        DailyRewardsUserData userData = user.getCachedModuleData(this.id, DailyRewardsUserData.class);
        return !userData.hasCollectedToday() && meetsRequiredPlaytime(player);
    }

    @Override
    public boolean claimRewards(Player player, RewardUser user) {
        if (!meetsRequiredPlaytime(player)) {
            ChatColorHandler.sendMessage(player, "&#ff6969You must have been online for " + requiredPlaytime + " minutes to claim these rewards");
            return false;
        }

        DailyRewardsUserData userData = user.getCachedModuleData(this.id, DailyRewardsUserData.class);
        if (userData == null) {
            ChatColorHandler.sendMessage(player, "&#ff6969Failed to collect your rewards data, try relogging. If this continues inform an administrator");
            LushRewards.getInstance().getLogger().warning("Failed to collect '%s' module user data for '%s'".formatted(this.id, player.getName()));
            return false;
        } else if (userData.hasCollectedToday()) {
            return false;
        }

        LocalDate lastCollectedDate = userData.getLastCollectedDate();
        boolean missedDay = lastCollectedDate == null || (lastCollectedDate.isBefore(LocalDate.now().minusDays(1)) && !lastCollectedDate.isEqual(DailyRewardsUserData.NEVER_COLLECTED));


        if (missedDay && !streakBypass) {
            userData.setStreak(1);
        } else {
            userData.incrementStreak();
        }

        userData.setLastCollectedDate(LocalDate.now());
        userData.addCollectedDay(userData.getDayNum());
        LushRewards.getInstance().getStorageManager().saveRewardUser(user).whenComplete((ignored, e) -> {
                if (e != null) {
                    LushRewards.getInstance().getLogger().log(Level.SEVERE, "Something went wrong when saving data for '%s' (%s)"
                        .formatted(player.getName(), player.getUniqueId()), e);
                    return;
                }

                RewardDay rewardDay = getRewardDay(LocalDate.now(), userData.getDayNum());
                DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

                Debugger.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), Debugger.DebugMode.DAILY);

                if (shouldStackRewards()) {
                    rewardDay.giveAllRewards(player);
                } else {
                    priorityReward.giveAll(player);
                }

                Debugger.sendDebugMessage("Successfully gave rewards to " + player.getName(), Debugger.DebugMode.DAILY);
                ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("daily-reward-given"));

                player.playSound(player.getLocation(), priorityReward.getSound() != null ? priorityReward.getSound() : this.getDefaultRedeemSound(), 1f, 1f);
            });

        return true;
    }

    @Override
    public boolean requiresPlaytimeTracker() {
        return requiredPlaytime != null;
    }

    public void checkRewardDay(UUID uuid, DailyRewardsUserData userData) {
        LocalDate lastJoinDate = userData.getLastJoinDate();
        if (lastJoinDate == null) {
            userData.setLastJoinDate(LocalDate.now());
            LushRewards.getInstance().getStorageManager().saveCachedRewardUser(uuid);
            return;
        } else if (lastJoinDate.isEqual(LocalDate.now())) {
            return;
        }

        LocalDate lastCollectedDate = userData.getLastCollectedDate();
        boolean missedDay = lastCollectedDate == null || (lastCollectedDate.isBefore(LocalDate.now().minusDays(1)) && !lastCollectedDate.isEqual(DailyRewardsUserData.NEVER_COLLECTED));

        switch (getRewardMode()) {
            case STREAK -> {
                // Resets RewardUser to Day 1 if a day has been missed
                if (missedDay) {
                    userData.setDayNum(1);
                    userData.setStreak(1);
                    userData.clearCollectedDays();
                } else {
                    userData.incrementDayNum();
                }
            }
            case ON_CLAIM_ONLY -> {
                if (userData.hasCollectedDay(userData.getDayNum()) && !userData.hasCollectedToday()) {
                    userData.incrementDayNum();
                }
            }
            case ONLINE_ONLY -> userData.incrementDayNum();
            case DEFAULT -> userData.setDayNum((int) (LocalDate.now().toEpochDay() - userData.getStartDate().toEpochDay()) + 1);
        }

        if (missedDay && !streakBypass) {
            userData.setStreak(1);
        }

        int resetDay = getResetDay();
        if (resetDay > 0 && userData.getDayNum() > resetDay) {
            userData.setStartDate(LocalDate.now());
            userData.setDayNum(1);
            userData.clearCollectedDays();
        }

        userData.setLastJoinDate(LocalDate.now());
        LushRewards.getInstance().getStorageManager().saveCachedRewardUser(uuid);
    }

    @NotNull
    public Collection<DailyRewardCollection> getDayNumRewards(int day) {
        return rewards.stream().filter(rewardCollection -> rewardCollection.isAvailableOn(day)).toList();
    }

    @NotNull
    public Collection<DailyRewardCollection> getDateRewards(LocalDate date) {
        return rewards.stream().filter(rewardCollection -> rewardCollection.isAvailableOn(date)).toList();
    }

    public RewardDay getRewardDay(LocalDate date, int streakDay) {
        RewardDay rewardDay = new RewardDay();
        rewardDay.addCollections(getDateRewards(date));
        rewardDay.addCollections(getDayNumRewards(streakDay));
        return rewardDay;
    }


    /**
     * @param day      Starting day of search (inclusive)
     * @param date     Starting date of search (inclusive)
     * @param category The category to search for
     * @return The reward found
     */
    public Optional<DailyRewardCollection> findNextRewardFromCategory(int day, LocalDate date, String category) {
        return rewards.stream()
            .filter(reward ->
                reward.getCategory().equalsIgnoreCase(category)
                    && ((reward.getRewardDayNum() != null && reward.getRewardDayNum() >= day)
                    || (reward.getRewardDate() != null && !reward.getRewardDate().isBefore(date))
                )
            )
            .min((reward1, reward2) -> {
                LocalDate date1 = reward1.getRewardDate();
                LocalDate date2 = reward2.getRewardDate();

                if (date1 == null) {
                    Integer dayNum1 = reward1.getRewardDayNum();

                    if (dayNum1 != null) {
                        date1 = date.plusDays(dayNum1 - day);
                    }
                }

                if (date2 == null) {
                    Integer dayNum2 = reward2.getRewardDayNum();

                    if (dayNum2 != null) {
                        date2 = date.plusDays(dayNum2 - day);
                    }
                }

                if (date1 == null || date2 == null) {
                    return date1 == null && date2 == null ? 0 : (date1 == null ? -1 : 1);
                }

                return date1.compareTo(date2);
            });
    }

    public int getResetDay() {
        return resetDaysAt;
    }

    public RewardMode getRewardMode() {
        return rewardMode;
    }

    public boolean shouldStackRewards() {
        return allowRewardsStacking;
    }

    public Sound getDefaultRedeemSound() {
        return defaultRedeemSound;
    }

    public String getUpcomingCategory() {
        return upcomingCategory;
    }

    public String getRewardPlaceholderClaimed() {
        return rewardPlaceholderClaimed;
    }

    public String getRewardPlaceholderUnclaimed() {
        return rewardPlaceholderUnclaimed;
    }

    @Override
    public Gui.Builder getGui() {
        return gui;
    }

    public enum RewardMode {
        DEFAULT,
        STREAK,
        ON_CLAIM_ONLY,
        ONLINE_ONLY
    }
}
