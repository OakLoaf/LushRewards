package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private FileConfiguration config;
    private final HashMap<String, ItemStack> categoryItems = new HashMap<>();
    private GuiTemplate guiTemplate;
    private ItemStack collectedItem;
    private ItemStack borderItem;
    private UpcomingReward upcomingReward;
    private boolean dailyRewardsEnabled;
    private boolean hourlyRewardsEnabled;
    private int loopLength;
    private int reminderPeriod;
    private boolean daysReset;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));

        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        if (templateType.equals("CUSTOM")) guiTemplate = new GuiTemplate(config.getStringList("gui.format"));
        else guiTemplate = GuiTemplate.DefaultTemplate.valueOf(templateType);

        collectedItem = ConfigParser.getItem(config.getConfigurationSection("gui.collected-item"), Material.REDSTONE_BLOCK);
        borderItem = ConfigParser.getItem(config.getConfigurationSection("gui.border-item"), Material.GRAY_STAINED_GLASS_PANE);

        boolean showUpcomingReward = config.getBoolean("gui.upcoming-reward.enabled", true);
        List<String> upcomingRewardLore = config.getStringList("gui.upcoming-reward.lore");
        upcomingReward = new UpcomingReward(showUpcomingReward, upcomingRewardLore);

        dailyRewardsEnabled = config.getBoolean("daily-rewards-enabled", true);
        hourlyRewardsEnabled = config.getBoolean("hourly-rewards-enabled", true);
        loopLength = config.getInt("loop-length", -1);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        daysReset = config.getBoolean("days-reset", false);

        reloadCategoryMap();
        notificationHandler.reloadNotifications(reminderPeriod);
        if (ActivityRewarder.getRewardManager() != null) ActivityRewarder.getRewardManager().reloadRewards();
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public String getRewardMessage() {
        return config.getString("messages.daily-reward-given", "&e&lRewards &8» &aYou have collected today's reward");
    }

    public String getBonusMessage() {
        return config.getString("messages.hourly-bonus-given", "&e&lRewards &8» &7You have received a bonus for playing &e%hours% &7hours");
    }

    public String getGuiTitle() {
        return config.getString("gui.title", "&8&lDaily Rewards");
    }

    public GuiTemplate getGuiTemplate() {
        return guiTemplate;
    }

    public boolean showUpcomingReward() {
        return upcomingReward.enabled;
    }

    public List<String> getUpcomingRewardLore() {
        return upcomingReward.lore;
    }

    public String getGuiItemRedeemableName(int day) {
        return config.getString("gui.redeemable-name", "&6Day %day%").replaceAll("%day%", String.valueOf(day));
    }

    public String getGuiItemCollectedName(int day) {
        return config.getString("gui.collected-name", "&6Day %day% - Collected").replaceAll("%day%", String.valueOf(day));
    }

    public ItemStack getCategoryItem(String category) {
        ItemStack item = categoryItems.get(category.toLowerCase());
        return item != null ? item.clone() : new ItemStack(Material.CHEST_MINECART);
    }

    public ItemStack getCollectedItem() {
        return collectedItem.clone();
    }

    public ItemStack getBorderItem() {
        return borderItem.clone();
    }

    public boolean areDailyRewardsEnabled() {
        return dailyRewardsEnabled;
    }

    public boolean areHourlyRewardsEnabled() {
        return hourlyRewardsEnabled;
    }

    public int getLoopLength() {
        return loopLength;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public boolean doDaysReset() {
        return daysReset;
    }

    public double getHourlyMultiplier(Player player) {
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        if (hourlySection == null || !hourlySection.getBoolean("enabled", false)) return 1;

        double heighestMultiplier = 1;
        for (String perm : hourlySection.getKeys(false)) {
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                double multiplier = hourlySection.getDouble(perm + ".multiplier", 1);
                if (multiplier > heighestMultiplier) heighestMultiplier = multiplier;
            }
        }

        return heighestMultiplier;
    }

    private void reloadCategoryMap() {
        // Clears category map
        categoryItems.clear();

        ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
        if (categoriesSection == null) return;

        // Repopulates category map
        for (String category : categoriesSection.getKeys(false)) {
            categoryItems.put(category, ConfigParser.getItem(categoriesSection.getConfigurationSection(category), Material.STONE));
        }
    }

    public record UpcomingReward(boolean enabled, List<String> lore) { }
}
