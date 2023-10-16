package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.Debugger;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlaytimeRewardCollection extends RewardCollection {
    private final int minutes;
    private final Integer repeatFrequency;
    private final Integer repeatsUntil;

    public PlaytimeRewardCollection(int minutes, @Nullable Integer repeatFrequency, @Nullable Integer repeatsUntil, @Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack displayItem, @Nullable Sound sound) {
        super(rewards, priority, category, displayItem, sound);
        this.minutes = minutes;
        this.repeatFrequency = repeatFrequency != null && repeatFrequency != 0 ? repeatFrequency : (repeatsUntil != null ? 1 : 0);
        this.repeatsUntil = repeatsUntil;
    }

    public int isAvailableAt(int totalMinutes) {
        return isAvailableAt(0, totalMinutes);
    }

    public int isAvailableAt(int lastCollected, int totalMinutes) {
        int amount = 0;
        int repeatsUntil = this.repeatsUntil != null ? this.repeatsUntil : Integer.MAX_VALUE;

        if (minutes > lastCollected && minutes < totalMinutes) {
            amount++;
        }

        if (repeatFrequency <= 0 || totalMinutes < minutes || lastCollected > repeatsUntil) {
            return amount;
        }

        return amount + (Math.min(totalMinutes, repeatsUntil) - minutes) / repeatFrequency;
    }

    @NotNull
    public static PlaytimeRewardCollection from(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.PLAYTIME;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        int minutes = rewardCollectionSection.getInt("play-minutes", 0);
        Debugger.sendDebugMessage("Reward collection minutes set to " + minutes, debugMode);

        int repeatFrequency = rewardCollectionSection.getInt("repeat", 0);
        Debugger.sendDebugMessage("Reward collection repeat frequency set to " + repeatFrequency, debugMode);

        Integer repeatsUntil = rewardCollectionSection.getInt("repeats-until", -1);

        String category = rewardCollectionSection.getString("category", "no-category");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        SimpleItemStack itemStack = itemSection != null ? SimpleItemStack.from(itemSection) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = ConfigParser.getSound(rewardCollectionSection.getString("redeem-sound", "none").toUpperCase());

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + ".rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return new PlaytimeRewardCollection(minutes, repeatFrequency, repeatsUntil, rewardList, priority, category, itemStack, redeemSound);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static PlaytimeRewardCollection from(Map<?, ?> rewardCollectionMap) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionMap + "'", debugMode);

        int priority = rewardCollectionMap.containsKey("priority") ? (int) rewardCollectionMap.get("priority") : 0;
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        int minutes = rewardCollectionMap.containsKey("play-minutes") ? (int) rewardCollectionMap.get("play-minutes") : 0;
        Debugger.sendDebugMessage("Reward collection minutes set to " + minutes, debugMode);

        int repeatFrequency = rewardCollectionMap.containsKey("repeat") ? (int) rewardCollectionMap.get("repeat") : 0;
        Debugger.sendDebugMessage("Reward collection repeat frequency set to " + repeatFrequency, debugMode);

        Integer repeatsUntil = rewardCollectionMap.containsKey("repeats-until") ? (int) rewardCollectionMap.get("repeats-until") : null;

        String category = rewardCollectionMap.containsKey("category") ? (String) rewardCollectionMap.get("category") : "small";
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        Map<?, ?> itemMap = (Map<?, ?>) rewardCollectionMap.get("display-item");
        SimpleItemStack itemStack = itemMap != null ? SimpleItemStack.from(itemMap) : new SimpleItemStack();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = rewardCollectionMap.containsKey("redeem-sound") ? ConfigParser.getSound((String) rewardCollectionMap.get("redeem-sound")) : Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = (List<Map<?, ?>>) rewardCollectionMap.get("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionMap.toString()) : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionMap + "'", debugMode);

        return rewardList != null ? new PlaytimeRewardCollection(minutes, repeatFrequency, repeatsUntil, rewardList, priority, category, itemStack, redeemSound) : PlaytimeRewardCollection.empty();
    }

    public static PlaytimeRewardCollection empty() {
        return new PlaytimeRewardCollection(0, null, null, null, 0, null, null, null);
    }
}
