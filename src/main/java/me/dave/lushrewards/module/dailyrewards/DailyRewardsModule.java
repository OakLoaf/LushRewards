package me.dave.lushrewards.module.dailyrewards;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.data.RewardUser;
import me.dave.lushrewards.exceptions.InvalidRewardException;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardDay;
import me.dave.lushrewards.utils.Debugger;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.module.Module;
import me.dave.platyutils.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DailyRewardsModule extends RewardModule<DailyRewardsModule.UserData> {
    private HashSet<DailyRewardCollection> rewards;
    private int resetDaysAt;
    private boolean streakMode;
    private boolean allowRewardsStacking;
    private Sound defaultRedeemSound;
    private String upcomingCategory;
    private boolean dateAsAmount;
    private DailyRewardsGui.ScrollType scrollType;
    private GuiFormat guiFormat;

    public DailyRewardsModule(String id, File moduleFile) {
        super(id, moduleFile, UserData.class, () -> new UserData(id, 0, 0, LocalDate.now(), null, new HashSet<>()));
    }

    @Override
    public void onEnable() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(moduleFile);
        if (!config.getBoolean("enabled", true)) {
            LushRewards.getInstance().getLogger().info("Module '" + id + "' is disabled in it's configuration");
            this.disable();
            return;
        }

        ConfigurationSection configurationSection = config.getConfigurationSection("daily-rewards");
        if (configurationSection == null) {
            LushRewards.getInstance().getLogger().severe("Failed to load rewards, could not find 'daily-rewards' section in '" + moduleFile.getName() + "'");
            this.disable();
            return;
        }

        this.resetDaysAt = config.getInt("reset-days-at", -1);
        this.streakMode = config.getBoolean("streak-mode", false);
        this.allowRewardsStacking = config.getBoolean("allow-reward-stacking", true);
        this.defaultRedeemSound = StringUtils.getEnum(config.getString("default-redeem-sound", "none"), Sound.class).orElse(null);
        this.upcomingCategory = config.getString("upcoming-category");

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        this.dateAsAmount = config.getBoolean("gui.date-as-amount", false);
        this.scrollType = DailyRewardsGui.ScrollType.valueOf(config.getString("gui.scroll-type", "MONTH").toUpperCase());
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiFormat.GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiFormat.GuiTemplate(config.getStringList("gui.format")) : GuiFormat.GuiTemplate.DefaultTemplate.valueOf(templateType);
        this.guiFormat = new GuiFormat(guiTitle, guiTemplate);

        this.rewards = new HashSet<>();

        LocalDate today = LocalDate.now();
        for (Object entry : configurationSection.getValues(false).values()) {
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
                            rewards.add(dailyRewardCollection);
                            break;
                        }
                    }
                } else {
                    rewards.add(dailyRewardCollection);
                }
            }
        }

        LushRewards.getInstance().getLogger().info("Successfully loaded " + rewards.size() + " reward collections from '" + configurationSection.getCurrentPath() + "'");
    }

    @Override
    public void onDisable() {
        if (rewards != null) {
            rewards.clear();
            rewards = null;
        }

        guiFormat = null;
    }

    public boolean claimRewards(Player player) {
        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(player);
        UserData userData = userDataMap.get(player.getUniqueId());
        if (userData == null || userData.hasCollectedToday()) {
            return false;
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

        player.playSound(player.getLocation(), priorityReward.getSound(), 1f, 1f);
        userData.incrementStreakLength();
        userData.setLastCollectedDate(LocalDate.now());
        userData.addCollectedDate(LocalDate.now());
        rewardUser.save();

        return true;
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

    public static class UserData extends RewardModule.UserData {
        private int streakLength;
        private int highestStreak;
        private LocalDate startDate;
        private LocalDate lastCollectedDate;
        private final HashSet<String> collectedDates;

        public UserData(String id, int streakLength, int highestStreak, LocalDate startDate, LocalDate lastCollectedDate, HashSet<String> collectedDates) {
            super(id);
            this.streakLength = streakLength;
            this.highestStreak = highestStreak;
            this.startDate = startDate;
            this.lastCollectedDate = lastCollectedDate;
            this.collectedDates = collectedDates;
        }

        public int getDayNum() {
            int dayNum = (int) (LocalDate.now().toEpochDay() - startDate.toEpochDay()) + 1;

            Optional<Module> optionalModule = LushRewards.getInstance().getModule(id);
            if (optionalModule.isPresent() && optionalModule.get() instanceof DailyRewardsModule dailyRewardsModule) {
                int resetDay = dailyRewardsModule.getResetDay();

                if (resetDay > 0 && dayNum > resetDay) {
                    setDayNum(1);
                    dayNum = 1;
                }
            }

            return dayNum;
        }

        public void setDayNum(int dayNum) {
            startDate = LocalDate.now().minusDays(dayNum - 1);
        }

        public int getStreakLength() {
            return streakLength;
        }

        public void setStreakLength(int streakLength) {
            this.streakLength = streakLength;
        }

        public void restartStreak() {
            setDayNum(1);
            setStreakLength(1);
            clearCollectedDates();
        }

        public void incrementStreakLength() {
            this.streakLength += 1;
            if (streakLength > highestStreak) {
                highestStreak = streakLength;
            }
        }

        public int getHighestStreak() {
            return highestStreak;
        }

        public LocalDate getDateAtStreakLength(int streakLength) {
            return LocalDate.now().plusDays(streakLength - getDayNum());
        }

        public LocalDate getStartDate() {
            return this.startDate;
        }

        @Nullable
        public LocalDate getLastCollectedDate() {
            return this.lastCollectedDate;
        }

        public void setLastCollectedDate(LocalDate date) {
            this.lastCollectedDate = date;
        }

        public HashSet<String> getCollectedDates() {
            return collectedDates;
        }

        public boolean hasCollectedToday() {
            return lastCollectedDate != null && lastCollectedDate.isEqual(LocalDate.now());
        }

        public void addCollectedDate(LocalDate date) {
            collectedDates.add(date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        }

        public void clearCollectedDates() {
            collectedDates.clear();
        }
    }
}
