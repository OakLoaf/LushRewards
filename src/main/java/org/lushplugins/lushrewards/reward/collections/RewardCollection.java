package org.lushplugins.lushrewards.reward.collections;

import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushlib.utils.converter.MapConverter;
import org.lushplugins.lushlib.utils.converter.YamlConverter;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.lushlib.utils.StringUtils;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.rewardsapi.api.RewardsAPI;
import org.lushplugins.rewardsapi.api.reward.Reward;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RewardCollection {
    protected final Collection<Reward> rewards;
    protected final int priority;
    protected final String category;
    protected final DisplayItemStack displayItem;
    protected final Sound sound;

    public RewardCollection(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable DisplayItemStack displayItem, @Nullable Sound sound) {
        this.rewards = rewards != null ? rewards : Collections.emptyList();
        this.priority = priority;
        this.category = category;
        this.displayItem = displayItem;
        this.sound = sound;
    }

    public Collection<Reward> getRewards() {
        return rewards;
    }

    public int getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public DisplayItemStack getDisplayItem() {
        return displayItem != null ? displayItem : LushRewards.getInstance().getConfigManager().getCategoryTemplate(category);
    }

    public Sound getSound() {
        return sound;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public boolean isEmpty() {
        return rewards.isEmpty();
    }

    public void giveAll(Player player) {
        for (Reward reward : rewards) {
            try {
                reward.give(player);
            } catch (Exception e) {
                LushRewards.getInstance().getLogger().severe("Error occurred when giving reward (" +reward.toString() + ") to " + player.getName());
                e.printStackTrace();
            }
        }
    }

    @NotNull
    public static RewardCollection from(ConfigurationSection rewardCollectionSection) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        int priority = rewardCollectionSection.getInt("priority", 0);
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        String category = rewardCollectionSection.getString("category", "small");
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        ConfigurationSection itemSection = rewardCollectionSection.getConfigurationSection("display-item");
        DisplayItemStack displayItem = itemSection != null ? YamlConverter.getDisplayItem(itemSection) : DisplayItemStack.empty();
        Debugger.sendDebugMessage("Reward collection item set to: " + displayItem, debugMode);

        Sound redeemSound = StringUtils.getEnum(rewardCollectionSection.getString("redeem-sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), Sound.class).orElse(null);

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = rewardCollectionSection.getMapList("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? RewardsAPI.readRewards(rewardMaps, rewardCollectionSection.getCurrentPath() + ".rewards") : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionSection.getCurrentPath() + "'", debugMode);

        return rewardList != null ? new RewardCollection(rewardList, 0, category, displayItem, redeemSound) : RewardCollection.empty();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static RewardCollection from(Map<?, ?> rewardCollectionMap) {
        Debugger.DebugMode debugMode = Debugger.DebugMode.DAILY;
        Debugger.sendDebugMessage("Attempting to load reward collection at '" + rewardCollectionMap + "'", debugMode);

        int priority = rewardCollectionMap.containsKey("priority") ? (int) rewardCollectionMap.get("priority") : 0;
        Debugger.sendDebugMessage("Reward collection priority set to " + priority, debugMode);

        String category = rewardCollectionMap.containsKey("category") ? (String) rewardCollectionMap.get("category") : "small";
        Debugger.sendDebugMessage("Reward collection category set to " + category, debugMode);

        Map<?, ?> itemMap = (Map<?, ?>) rewardCollectionMap.get("display-item");
        DisplayItemStack itemStack = itemMap != null ? MapConverter.getDisplayItemBuilder(itemMap).build() : DisplayItemStack.empty();
        Debugger.sendDebugMessage("Reward collection item set to: " + itemStack, debugMode);

        Sound redeemSound = rewardCollectionMap.containsKey("redeem-sound") ? StringUtils.getEnum((String) rewardCollectionMap.get("redeem-sound"), Sound.class).orElse(null) : Sound.ENTITY_EXPERIENCE_ORB_PICKUP;

        Debugger.sendDebugMessage("Attempting to load rewards", debugMode);
        List<Map<?, ?>> rewardMaps = (List<Map<?, ?>>) rewardCollectionMap.get("rewards");

        List<Reward> rewardList = !rewardMaps.isEmpty() ? RewardsAPI.readRewards(rewardMaps, rewardCollectionMap.toString()) : null;
        Debugger.sendDebugMessage("Successfully loaded " + (rewardList != null ? rewardList.size() : 0) + " rewards from '" + rewardCollectionMap + "'", debugMode);

        return rewardList != null ? new RewardCollection(rewardList, priority, category, itemStack, redeemSound) : RewardCollection.empty();
    }

    public static RewardCollection empty() {
        return new RewardCollection(null, 0, null, null, null);
    }
}
