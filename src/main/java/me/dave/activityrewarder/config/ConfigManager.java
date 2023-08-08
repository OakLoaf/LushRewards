package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private final HashMap<String, SimpleItemStack> categoryItems = new HashMap<>();
    private final HashMap<String, SimpleItemStack> itemTemplates = new HashMap<>();
    private final HashMap<String, String> messages = new HashMap<>();
    private GuiFormat guiFormat;
    private UpcomingRewardFormat upcomingRewardFormat;
    private boolean dailyRewardsEnabled;
    private boolean hourlyRewardsEnabled;
    private boolean rewardsRefresh;
    private int reminderPeriod;
    private boolean daysReset;

    public ConfigManager() {
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiTemplate(config.getStringList("gui.format")) : GuiTemplate.DefaultTemplate.valueOf(templateType);

        guiFormat = new GuiFormat(guiTitle, guiTemplate);

        boolean showUpcomingReward = config.getBoolean("gui.upcoming-reward.enabled", true);
        List<String> upcomingRewardLore = config.getStringList("gui.upcoming-reward.lore");
        upcomingRewardFormat = new UpcomingRewardFormat(showUpcomingReward, upcomingRewardLore);

        dailyRewardsEnabled = config.getBoolean("daily-rewards-enabled", true);
        hourlyRewardsEnabled = config.getBoolean("hourly-rewards-enabled", true);
        rewardsRefresh = config.getBoolean("rewards-refresh-daily", false);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        daysReset = config.getBoolean("days-reset", false);

        reloadCategoryMap(config.getConfigurationSection("categories"));
        reloadItemTemplates(config.getConfigurationSection("item-templates"));
        reloadMessages(config.getConfigurationSection("messages"));
        notificationHandler.reloadNotifications(reminderPeriod);
        if (ActivityRewarder.getRewardManager() != null) ActivityRewarder.getRewardManager().reloadRewards();
    }

    public String getMessage(String messageName) {
        return messages.getOrDefault(messageName, "");
    }

    public GuiFormat getGuiFormat() {
        return guiFormat;
    }

    public UpcomingRewardFormat getUpcomingRewardFormat() {
        return upcomingRewardFormat;
    }

    public SimpleItemStack getCategoryItem(String category) {
        return categoryItems.get(category.toLowerCase());
    }

    public SimpleItemStack getItemTemplate(String key) {
        return itemTemplates.getOrDefault(key, new SimpleItemStack());
    }

    public boolean areDailyRewardsEnabled() {
        return dailyRewardsEnabled;
    }

    public boolean areHourlyRewardsEnabled() {
        return hourlyRewardsEnabled;
    }

    public boolean doRewardsRefresh() {
        return rewardsRefresh;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public boolean doDaysReset() {
        return daysReset;
    }

    private void reloadCategoryMap(ConfigurationSection categoriesSection) {
        // Clears category map
        categoryItems.clear();

        // Checks if categories section exists
        if (categoriesSection == null) return;

        // Repopulates category map
        categoriesSection.getValues(false).entrySet().forEach((data) -> {
            if (data instanceof ConfigurationSection categorySection) {
                categoryItems.put(categorySection.getName(), SimpleItemStack.from(categorySection, Material.STONE));
            }
        });
    }

    private void reloadItemTemplates(ConfigurationSection itemTemplatesSection) {
        // Clears category map
        itemTemplates.clear();

        // Checks if categories section exists
        if (itemTemplatesSection == null) return;

        // Repopulates category map
        itemTemplatesSection.getValues(false).entrySet().forEach((data) -> {
            if (data instanceof ConfigurationSection categorySection) {
                itemTemplates.put(categorySection.getName(), SimpleItemStack.from(categorySection, Material.STONE));
            }
        });
    }

    private void reloadMessages(ConfigurationSection messagesSection) {
        // Clears messages map
        messages.clear();

        // Checks if messages section exists
        if (messagesSection == null) return;

        // Repopulates messages map
        messagesSection.getValues(false).forEach((key, value) -> {
            messages.put(key, (String) value);
        });
    }

    public record GuiFormat(String title, GuiTemplate template) {}
    public record UpcomingRewardFormat(boolean enabled, List<String> lore) {}
}
