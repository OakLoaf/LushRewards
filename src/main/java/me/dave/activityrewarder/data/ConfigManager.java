package me.dave.activityrewarder.data;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.NotificationHandler;
import me.dave.activityrewarder.gui.GuiTemplate;
import me.dave.activityrewarder.rewards.RewardCollection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.dave.activityrewarder.rewards.custom.CmdReward;
import me.dave.activityrewarder.rewards.custom.ItemReward;
import me.dave.activityrewarder.rewards.Reward;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.logging.Logger;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final Logger logger = plugin.getLogger();
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private FileConfiguration config;
    private DebugMode debugMode;
    private RewardCollection defaultReward;
    private final HashMap<Integer, RewardCollection> dayToRewards = new HashMap<>();
    private final HashMap<String, ItemStack> sizeItems = new HashMap<>();
    private GuiTemplate guiTemplate;
    private ItemStack collectedItem;
    private ItemStack borderItem;
    private UpcomingReward upcomingReward;
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

        debugMode = DebugMode.valueOf(config.getString("debug-mode", "NONE").toUpperCase());

        String templateType = config.getString("gui.template", "DEFAULT").toUpperCase();
        if (templateType.equals("CUSTOM")) guiTemplate = new GuiTemplate(config.getStringList("gui.format"));
        else guiTemplate = GuiTemplate.DefaultTemplate.valueOf(templateType);

        collectedItem = getItem(config.getString("gui.collected-item", "REDSTONE_BLOCK").split(";"), Material.REDSTONE_BLOCK);
        borderItem = getItem(config.getString("gui.border-item", "GRAY_STAINED_GLASS_PANE").split(";"), Material.GRAY_STAINED_GLASS_PANE);

        boolean showUpcomingReward = config.getBoolean("gui.upcoming-reward.enabled", true);
        List<String> upcomingRewardLore = config.getStringList("gui.upcoming-reward.lore");
        upcomingReward = new UpcomingReward(showUpcomingReward, upcomingRewardLore);

        loopLength = config.getInt("loop-length", -1);
        reminderPeriod = config.getInt("reminder-period", 1800) * 20;
        daysReset = config.getBoolean("days-reset", false);


        reloadRewardsMap();
        reloadSizeMap();
        notificationHandler.reloadNotifications(reminderPeriod);
    }

    public void sendDebugMessage(String string, DebugMode mode) {
        if (debugMode == mode || debugMode == DebugMode.ALL) logger.info("DEBUG >> " + string);
    }

    public String getReloadMessage() {
        return config.getString("messages.reload", "&aConfig reloaded");
    }

    public String getReminderMessage() {
        return config.getString("messages.reminder", "&e&lRewards &8» &7It looks like you haven't collected today's reward from &e/rewards");
    }

    public String getRewardMessage() {
        return config.getString("messages.reward-given", "&e&lRewards &8» &aYou have collected today's reward");
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

    public ItemStack getSizeItem(String size) {
        return sizeItems.get(size.toLowerCase()).clone();
    }

    public ItemStack getCollectedItem() {
        return collectedItem.clone();
    }

    public ItemStack getBorderItem() {
        return borderItem.clone();
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
                double multiplier = hourlySection.getConfigurationSection(perm).getDouble("multiplier", 1);
                if (multiplier > heighestMultiplier) heighestMultiplier = multiplier;
            }
        }

        return heighestMultiplier;
    }

    public RewardCollection getHourlyRewards(Player player) {
        sendDebugMessage("Getting hourly bonus section from config", DebugMode.HOURLY);
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        if (hourlySection == null) return null;
        sendDebugMessage("Checking if hourly bonus is enabled", DebugMode.HOURLY);
        if (!hourlySection.getBoolean("enabled", false)) return null;
        RewardCollection hourlyRewards = null;

        sendDebugMessage("Checking player's highest multiplier", DebugMode.HOURLY);
        double heighestMultiplier = 0;
        for (String perm : hourlySection.getKeys(false)) {
            sendDebugMessage("Checking if player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                sendDebugMessage("Player has activityrewarder.bonus." + perm, DebugMode.HOURLY);
                double multiplier = hourlySection.getConfigurationSection(perm).getDouble("multiplier", 1);

                if (multiplier > heighestMultiplier) {
                    sendDebugMessage("Found higher multiplier, updated highest multiplier", DebugMode.HOURLY);
                    heighestMultiplier = multiplier;
                    hourlyRewards = loadSectionRewards(hourlySection.getConfigurationSection(perm), DebugMode.HOURLY);
                }
            }
        }
        sendDebugMessage("Found highest multiplier (" + heighestMultiplier + ")", DebugMode.HOURLY);
        RewardUser rewardUser = ActivityRewarder.dataManager.getRewardUser(player.getUniqueId());
        rewardUser.setHourlyMultiplier(heighestMultiplier);

        return hourlyRewards;
    }

    public RewardCollection getRewards(int day) {
        // Works out what day number the user is in the loop
        int loopedDayNum = day;
        if (day > getLoopLength()) {
            loopedDayNum = (day % getLoopLength()) + 1;
        }

        if (dayToRewards.containsKey(day)) return dayToRewards.get(day);
        else if (dayToRewards.containsKey(loopedDayNum)) return dayToRewards.get(loopedDayNum);
        else return defaultReward;
    }

    public int findNextRewardOfSize(int day, String size) {
        int nextRewardKey = -1;

        // Iterates through dayToRewards
        for (int rewardsKey : dayToRewards.keySet()) {
            // Checks if the current key is a day in the future
            if (rewardsKey <= day || (nextRewardKey != -1 && rewardsKey > nextRewardKey)) continue;

            // Gets the size of the reward and compares to the request
            RewardCollection rewards = getRewards(rewardsKey);
            if (rewards.getSize().equalsIgnoreCase(size)) nextRewardKey = rewardsKey;
        }

        // Returns -1 if no future rewards match the request
        return nextRewardKey;
    }

    private void reloadRewardsMap() {
        // Clears rewards map
        dayToRewards.clear();

        ConfigurationSection rewardDaysSection = config.getConfigurationSection("reward-days");
        if (rewardDaysSection == null) rewardDaysSection = config.getConfigurationSection("rewards");
        for (String rewardDayKey : rewardDaysSection.getKeys(false)) {
            if (rewardDayKey.equalsIgnoreCase("default")) {
                defaultReward = loadSectionRewards(rewardDaysSection.getConfigurationSection(rewardDayKey), DebugMode.DAILY);
                continue;
            }
            dayToRewards.put(Integer.parseInt(rewardDayKey), loadSectionRewards(rewardDaysSection.getConfigurationSection(rewardDayKey), DebugMode.DAILY));
        }
    }

    private void reloadSizeMap() {
        // Clears size map
        sizeItems.clear();

        ConfigurationSection sizesSection = config.getConfigurationSection("sizes");
        for (String sizeKey : sizesSection.getKeys(false)) {
            String[] materialDataArr = sizesSection.getString(sizeKey, "STONE").split(";");
            sizeItems.put(sizeKey, getItem(materialDataArr, Material.STONE));
        }
    }

    private RewardCollection loadSectionRewards(ConfigurationSection rewardDaySection, DebugMode debugMode) {
        sendDebugMessage("Attempting to load sections reward (" + rewardDaySection.getCurrentPath() + ")", debugMode);
        ArrayList<Reward> rewards = new ArrayList<>();
        String size = rewardDaySection.getString("size", "SMALL").toUpperCase();
        sendDebugMessage("Reward size set (" + size + ")", debugMode);
        List<String> lore = new ArrayList<>();
        if (rewardDaySection.getKeys(false).contains("lore")) {
            lore = rewardDaySection.getStringList("lore");
        }
        sendDebugMessage("Lore set", debugMode);

        sendDebugMessage("Attempting to load item rewards", debugMode);
        ConfigurationSection itemRewards = rewardDaySection.getConfigurationSection("rewards.items");
        int itemRewardCount = 0;
        if (itemRewards != null) {
            for (String materialName : itemRewards.getKeys(false)) {
                ItemStack item = getItem(materialName.toUpperCase(), Material.GOLD_NUGGET);
                int amount = itemRewards.getInt(materialName + ".amount", 1);
                item.setAmount(amount);

                rewards.add(new ItemReward(item));
                itemRewardCount++;
            }
        }
        sendDebugMessage("Successfully loaded " + itemRewardCount + " item rewards", debugMode);

        sendDebugMessage("Attempting to load command rewards", debugMode);
        int cmdRewardCount = 0;
        for (String command : rewardDaySection.getStringList("rewards.commands")) {
            rewards.add(new CmdReward(command));
            cmdRewardCount++;
        }
        sendDebugMessage("Successfully loaded " + cmdRewardCount + " command rewards", debugMode);

        sendDebugMessage("Successfully loaded " + (itemRewardCount + cmdRewardCount) + " total rewards", debugMode);
        return new RewardCollection(size, lore, rewards);
    }

    @Nullable
    private Material getMaterial(String materialName) {
        return getMaterial(materialName, null);
    }

    private Material getMaterial(String materialName, Material def) {
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException err) {
            plugin.getLogger().warning("Ignoring " + materialName + ", that is not a valid material.");
            if (def != null) {
                material = def;
                plugin.getLogger().warning("Defaulted material to " + def.name() + ".");
            }
            else return null;
        }
        return material;
    }

    private ItemStack getItem(String materialName, Material def) {
        Material material = getMaterial(materialName, def);
        if (material == null) return null;
        return new ItemStack(material);
    }

    private @NotNull ItemStack getItem(String[] materialData) {
        return getItem(materialData, Material.STONE);
    }

    private @NotNull ItemStack getItem(String[] materialData, @NotNull Material def) {
        if (materialData.length == 0) return new ItemStack(def);
        Material material = getMaterial(materialData[0].toUpperCase(), def);
        if (material == null) material = def;

        ItemStack item = new ItemStack(material);

        if (materialData.length >= 2) {
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setCustomModelData(Integer.parseInt(materialData[1]));
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    public record UpcomingReward(boolean enabled, List<String> lore) { }
}
