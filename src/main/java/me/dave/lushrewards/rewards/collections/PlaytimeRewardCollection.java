package me.dave.lushrewards.rewards.collections;

import me.dave.lushrewards.rewards.custom.Reward;
import me.dave.lushrewards.utils.Debugger;
import org.lushplugins.lushlib.utils.SimpleItemStack;
import org.lushplugins.lushlib.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PlaytimeRewardCollection extends RewardCollection {
    private final int startMinute;
    private final int repeatFrequency;
    private final int repeatsUntil;
    private final boolean hideFromGui;

    public PlaytimeRewardCollection(int startMinute, @Nullable Integer repeatFrequency, @Nullable Integer repeatsUntil, @Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable SimpleItemStack displayItem, @Nullable Sound sound, boolean hideFromGui) {
        super(rewards, priority, category, displayItem, sound);
        this.startMinute = startMinute;
        this.repeatFrequency = repeatFrequency != null && repeatFrequency != 0 ? repeatFrequency : (repeatsUntil != null ? 1 : 0);
        this.repeatsUntil = repeatsUntil != null ? repeatsUntil : Integer.MAX_VALUE;;
        this.hideFromGui = hideFromGui;
    }

    public boolean isAvailableAt(int playtime) {
        return amountAvailableAt(playtime) > 0;
    }

    public boolean isAvailableAt(int prevPlaytime, int playtime) {
        return amountAvailableAt(prevPlaytime, playtime) > 0;
    }

    public int amountAvailableAt(int prevPlaytime, int playtime) {
        return amountAvailableAt(playtime) - amountAvailableAt(prevPlaytime);
    }

    public int amountAvailableAt(int playtime) {
        if (playtime < startMinute) {
            return 0;
        }

        if (repeatFrequency <= 0) {
            return startMinute < playtime ? 1 : 0;
        }

        int endMinute = Math.min(playtime, repeatsUntil);
        int playtimeSinceStart = endMinute - startMinute;
        int result = (playtimeSinceStart / repeatFrequency) + 1; // We add 1 to include the startMinute
        return Math.max(result, 0); // Ensure that the value is not less than 0
    }

    public int getStartMinute() {
        return startMinute;
    }

    public Integer getRepeatFrequency() {
        return repeatFrequency;
    }

    public Integer getRepeatsUntil() {
        return repeatsUntil;
    }

    public boolean shouldHideFromGui() {
        return hideFromGui;
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

        Sound redeemSound = StringUtils.getEnum(rewardCollectionSection.getString("redeem-sound", "none"), Sound.class).orElse(null);
        boolean hideFromGui = rewardCollectionSection.getBoolean("hide-from-gui");
        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + ".rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return new PlaytimeRewardCollection(minutes, repeatFrequency, repeatsUntil, rewardList, priority, category, itemStack, redeemSound, hideFromGui);
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

        Sound redeemSound = rewardCollectionMap.containsKey("redeem-sound") ? StringUtils.getEnum((String) rewardCollectionMap.get("redeem-sound"), Sound.class).orElse(Sound.ENTITY_EXPERIENCE_ORB_PICKUP) : Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        boolean showsInGui = rewardCollectionMap.containsKey("hide-from-gui") && (boolean) rewardCollectionMap.get("hide-from-gui");

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = (List<Map<?, ?>>) rewardCollectionMap.get("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, rewardCollectionMap.toString()) : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionMap + "'", debugMode);

        return rewardList != null ? new PlaytimeRewardCollection(minutes, repeatFrequency, repeatsUntil, rewardList, priority, category, itemStack, redeemSound, showsInGui) : PlaytimeRewardCollection.empty();
    }

    public static PlaytimeRewardCollection empty() {
        return new PlaytimeRewardCollection(0, null, null, null, 0, null, null, null, true);
    }
}
