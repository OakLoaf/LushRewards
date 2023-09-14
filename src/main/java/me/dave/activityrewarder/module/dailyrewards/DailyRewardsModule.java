package me.dave.activityrewarder.module.dailyrewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.gui.GuiFormat;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.collections.RewardDay;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DailyRewardsModule extends Module {
    private int rewardsIndex;
    private ConcurrentHashMap<Integer, DailyRewardCollection> rewards;
    private int resetDaysAt;
    private boolean streakMode;
    private boolean allowRewardsStacking;
    private Sound defaultRedeemSound;
    private String upcomingCategory;
    private boolean dateAsAmount;
    private DailyRewardsGui.ScrollType scrollType;
    private GuiFormat guiFormat;

    public DailyRewardsModule(String id) {
        super(id);
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = ActivityRewarder.getConfigManager().getDailyRewardsConfig();
        ConfigurationSection configurationSection = config.getConfigurationSection("daily-rewards");
        if (configurationSection == null) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section in 'daily-rewards.yml'");
            this.disable();
            return;
        }

        this.resetDaysAt = config.getInt("reset-days-at", -1);
        this.streakMode = config.getBoolean("streak-mode", false);
        this.allowRewardsStacking = config.getBoolean("allow-rewards-stacking", true);
        this.defaultRedeemSound = ConfigParser.getSound(config.getString("default-redeem-sound", "none").toUpperCase());
        this.upcomingCategory = config.getString("upcoming-category");

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        this.dateAsAmount = config.getBoolean("gui.date-as-amount", false);
        this.scrollType = DailyRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "MONTH").toUpperCase());
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.rewardsIndex = 0;
        this.rewards = new ConcurrentHashMap<>();

        LocalDate today = LocalDate.now();
        for (Map.Entry<String, Object> entry : configurationSection.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection rewardSection) {
                DailyRewardCollection dailyRewardCollection;
                try {
                    dailyRewardCollection = DailyRewardCollection.from(rewardSection);
                } catch (InvalidRewardException e) {
                    e.printStackTrace();
                    continue;
                }

                if (ActivityRewarder.getConfigManager().isPerformanceModeEnabled() && dailyRewardCollection.getRewardDate() != null) {
                    LocalDate lowestDate;
                    LocalDate highestDate;
                    switch (scrollType) {
                        case DAY -> {
                            lowestDate = LocalDate.now();
                            highestDate = today.plusDays(guiTemplate.countChar('R') - 1);
                        }
                        case MONTH -> {
                            lowestDate = LocalDate.of(today.getYear(), today.getMonthValue(), 1);
                            highestDate = LocalDate.of(today.getYear(), today.getMonthValue(), today.getMonth().length(today.isLeapYear()));
                        }
                        // Uses GRID mode by default as this has the largest range of possible dates
                        default -> {
                            int rewardDisplayCount = guiTemplate.countChar('R');

                            lowestDate = today.minusDays(rewardDisplayCount - 1);
                            highestDate = today.plusDays(rewardDisplayCount - 1);
                        }
                    }

                    for (LocalDate dateIndex = lowestDate; !dateIndex.isAfter(highestDate); dateIndex = dateIndex.plusDays(1)) {
                        if (dailyRewardCollection.isAvailableOn(dateIndex)) {
                            registerRewardCollection(dailyRewardCollection);
                            break;
                        }
                    }
                } else {
                    registerRewardCollection(dailyRewardCollection);
                }
            }
        }

        ActivityRewarder.getInstance().getLogger().info("Successfully loaded " + rewards.size() + " reward collections from '" + configurationSection.getCurrentPath() + "'");
    }

    @Override
    public void onDisable() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }

        guiFormat = null;
        rewardsIndex = 0;
    }

    public int registerRewardCollection(DailyRewardCollection collection) {
        rewardsIndex++;
        rewards.put(rewardsIndex, collection);
        return rewardsIndex;
    }

    @NotNull
    public Collection<DailyRewardCollection> getDayNumRewards(int day) {
        return rewards.values().stream().filter(rewardCollection -> rewardCollection.isAvailableOn(day)).toList();
    }

    @NotNull
    public Collection<DailyRewardCollection> getDateRewards(LocalDate date) {
        return rewards.values().stream().filter(rewardCollection -> rewardCollection.isAvailableOn(date)).toList();
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

        return rewards.values().stream()
                .filter(reward ->
                        !(reward.getCategory().equalsIgnoreCase(category)
                                || (reward.getRewardDayNum() != null && reward.getRewardDayNum() < day)
                                || (reward.getRewardDate() != null && reward.getRewardDate().isBefore(date))))
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

    public boolean isStreakModeEnabled() {
        return streakMode;
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

    public boolean showDateAsAmount() {
        return dateAsAmount;
    }

    public DailyRewardsGui.ScrollType getScrollType() {
        return scrollType;
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    public boolean claimRewards(Player player) {
        RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
        if (rewardUser.hasCollectedToday()) {
            return false;
        }

        RewardDay rewardDay = getRewardDay(LocalDate.now(), rewardUser.getDayNum());
        DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

        Debugger.sendDebugMessage("Attempting to send daily rewards to " + player.getName(), Debugger.DebugMode.DAILY);

        if (shouldStackRewards()) {
            rewardDay.giveAllRewards(player);
        } else {
            priorityReward.giveAll(player);
        }

        Debugger.sendDebugMessage("Successfully gave rewards to " + player.getName(), Debugger.DebugMode.DAILY);
        ChatColorHandler.sendMessage(player, ActivityRewarder.getConfigManager().getMessage("daily-reward-given"));

        player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
        rewardUser.incrementStreakLength();
        rewardUser.setLastCollectedDate(LocalDate.now());
        rewardUser.addCollectedDate(LocalDate.now());

        return true;
    }
}
