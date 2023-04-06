package me.dave.activityrewarder.datamanager;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.NotificationHandler;
import me.dave.activityrewarder.rewards.RewardsDay;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.dave.activityrewarder.rewards.CmdReward;
import me.dave.activityrewarder.rewards.ItemReward;
import me.dave.activityrewarder.rewards.Reward;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.logging.Logger;

public class ConfigManager {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();
    private final Logger logger = plugin.getLogger();
    private final NotificationHandler notificationHandler = new NotificationHandler();
    private FileConfiguration config;
    private DebugMode debugMode;
    private RewardsDay defaultReward;
    private final HashMap<Integer, RewardsDay> dayToRewards = new HashMap<>();
    private final HashMap<String, ItemStack> sizeItems = new HashMap<>();
    private ItemStack collectedItem;
    private ItemStack borderItem;
    private int guiRowCount;
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

        collectedItem = getItem(config.getString("collected-item", "REDSTONE_BLOCK").split(";"), "REDSTONE_BLOCK");
        borderItem = getItem(config.getString("gui.border-item", "GRAY_STAINED_GLASS_PANE").split(";"), "GRAY_STAINED_GLASS_PANE");

        guiRowCount = config.getInt("gui.row-count", 1);
        if (guiRowCount < 1) guiRowCount = 1;
        else if (guiRowCount > 4) guiRowCount = 4;

        boolean showUpcomingReward = config.getBoolean("gui.upcoming-reward.enabled", true);
        int upcomingRewardSlot = config.getInt("gui.upcoming-reward.slot", -5);
        List<String> upcomingRewardLore = config.getStringList("gui.upcoming-reward.lore");
        upcomingReward = new UpcomingReward(showUpcomingReward, upcomingRewardSlot, upcomingRewardLore);

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

    public int getGuiRowCount() {
        return guiRowCount;
    }

    public boolean showUpcomingReward() {
        return upcomingReward.enabled;
    }

    public int getUpcomingRewardSlot() {
        return upcomingReward.slot;
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
        if (hourlySection == null) return 1;

        double heighestMultiplier = 1;
        for (String perm : hourlySection.getKeys(false)) {
            if (player.hasPermission("activityrewarder.bonus." + perm)) {
                double multiplier = hourlySection.getConfigurationSection(perm).getDouble("multiplier", 1);
                if (multiplier > heighestMultiplier) heighestMultiplier = multiplier;
            }
        }

        return heighestMultiplier;
    }

    public RewardsDay getHourlyRewards(Player player) {
        sendDebugMessage("Getting hourly bonus section from config", DebugMode.HOURLY);
        ConfigurationSection hourlySection = config.getConfigurationSection("hourly-bonus");
        if (hourlySection == null) return null;
        RewardsDay hourlyRewards = null;

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

    public RewardsDay getRewards(int day) {
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
            RewardsDay rewards = getRewards(rewardsKey);
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
            sizeItems.put(sizeKey, getItem(materialDataArr));
        }
    }

    private RewardsDay loadSectionRewards(ConfigurationSection rewardDaySection, DebugMode debugMode) {
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
                ItemStack item = new ItemStack(getMaterial(materialName.toUpperCase(), "GOLD_NUGGET"));
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
        return new RewardsDay(size, lore, rewards);
    }

    private Material getMaterial(String materialName) {
        return getMaterial(materialName, null);
    }

    private Material getMaterial(String materialName, String def) {
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException err) {
            plugin.getLogger().warning("Ignoring " + materialName + ", that is not a valid material.");
                if (def != null) material = Material.valueOf(def);
                else material = Material.STONE;
        }
        return material;
    }

    private ItemStack getItem(String[] materialData) {
        return getItem(materialData, null);
    }

    private ItemStack getItem(String[] materialData, String def) {
        Material material = Material.STONE;
        if (def != null) material = Material.valueOf(def);

        if (materialData.length >= 1) material = getMaterial(materialData[0].toUpperCase(), "STONE");

        ItemStack item = new ItemStack(material);

        if (materialData.length >= 2) {
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setCustomModelData(Integer.parseInt(materialData[1]));
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    public record UpcomingReward(boolean enabled, int slot, List<String> lore) {}
}
