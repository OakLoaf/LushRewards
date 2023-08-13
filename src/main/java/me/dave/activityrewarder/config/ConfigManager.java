package me.dave.activityrewarder.config;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.notifications.NotificationHandler;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private final HashMap<String, SimpleItemStack> categoryItems = new HashMap<>();
    private final HashMap<String, SimpleItemStack> itemTemplates = new HashMap<>();
    private final HashMap<String, String> messages = new HashMap<>();
    private GuiFormat guiFormat;
    private boolean dailyRewardsEnabled;
    private boolean playtimeRewardsEnabled;
    private boolean allowRewardsStacking;
    private boolean rewardsRefresh;
    private int reminderPeriod;
    private boolean streakMode;
    private String upcomingCategory;

    public ConfigManager() {
        ActivityRewarder.getInstance().saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        ActivityRewarder.getInstance().reloadConfig();
        FileConfiguration config = ActivityRewarder.getInstance().getConfig();

        Debugger.setDebugMode(Debugger.DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase()));

        String guiTitle = config.getString("gui.title", "&8&lDaily Rewards");
        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        GuiTemplate guiTemplate = templateType.equals("CUSTOM") ? new GuiTemplate(config.getStringList("gui.format")) : GuiTemplate.DefaultTemplate.valueOf(templateType);

        guiFormat = new GuiFormat(guiTitle, guiTemplate);

        dailyRewardsEnabled = config.getBoolean("daily-rewards-enabled", true);
        playtimeRewardsEnabled = config.getBoolean("playtime-rewards-enabled", true);
        allowRewardsStacking = config.getBoolean("allow-rewards-stacking", true);
        rewardsRefresh = config.getBoolean("rewards-refresh-daily", false);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        streakMode = config.getBoolean("streak-mode", false);
        upcomingCategory = config.getString("upcoming-category");


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

    public SimpleItemStack getCategoryTemplate(String category) {
        SimpleItemStack itemTemplate = categoryItems.get(category.toLowerCase());
        if (itemTemplate == null) {
            ActivityRewarder.getInstance().getLogger().severe("Could not find category '" + category + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public String getUpcomingCategory() {
        return upcomingCategory;
    }

    public SimpleItemStack getItemTemplate(String key) {
        SimpleItemStack itemTemplate = itemTemplates.get(key);
        if (itemTemplate == null) {
            ActivityRewarder.getInstance().getLogger().severe("Could not find item-template '" + key + "'");
            return new SimpleItemStack();
        }

        return itemTemplate.clone();
    }

    public boolean areDailyRewardsEnabled() {
        return dailyRewardsEnabled;
    }

    public boolean arePlaytimeRewardsEnabled() {
        return playtimeRewardsEnabled;
    }

    public boolean shouldStackRewards() {
        return allowRewardsStacking;
    }

    public boolean doRewardsRefresh() {
        return rewardsRefresh;
    }

    public int getReminderPeriod() {
        return reminderPeriod;
    }

    public boolean isStreakModeEnabled() {
        return streakMode;
    }

    private void reloadCategoryMap(ConfigurationSection categoriesSection) {
        // Clears category map
        categoryItems.clear();

        // Checks if categories section exists
        if (categoriesSection == null) {
            return;
        }

        // Repopulates category map
        categoriesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                categoryItems.put(categorySection.getName(), SimpleItemStack.from(categorySection));
            }
        });
    }

    private void reloadItemTemplates(ConfigurationSection itemTemplatesSection) {
        // Clears category map
        itemTemplates.clear();

        // Checks if categories section exists
        if (itemTemplatesSection == null) {
            return;
        }

        // Repopulates category map
        itemTemplatesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                itemTemplates.put(categorySection.getName(), SimpleItemStack.from(categorySection));
                ActivityRewarder.getInstance().getLogger().info("Loaded item-template: " + categorySection.getName());
            }
        });
    }

    private void reloadMessages(ConfigurationSection messagesSection) {
        // Clears messages map
        messages.clear();

        // Checks if messages section exists
        if (messagesSection == null) {
            return;
        }

        // Repopulates messages map
        messagesSection.getValues(false).forEach((key, value) -> messages.put(key, (String) value));
    }

    public record GuiFormat(String title, GuiTemplate template) {}
    public record UpcomingRewardFormat(boolean enabled, List<String> lore) {}
}
