package me.dave.activityrewarder.rewards.collections;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DailyRewardCollection extends RewardCollection {
    private final int priority;
    private final String category;
    private final List<String> lore;
    private final Sound sound;

    private DailyRewardCollection(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable List<String> lore, @Nullable Sound sound) {
        super(rewards);
        this.priority = priority;
        this.category = category != null ? category : ActivityRewarder.getRewardManager().getDefaultReward().getCategory();
        this.lore = lore != null ? lore : ActivityRewarder.getRewardManager().getDefaultReward().getLore();
        this.sound = sound != null ? sound : ActivityRewarder.getRewardManager().getDefaultReward().getSound();
    }

    public int getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLore() {
        return lore;
    }

    public Sound getSound() {
        return sound;
    }

    public ItemStack getDisplayItem() {
        // Get the current reward's category item
        String category = this.category;
        ItemStack item = ActivityRewarder.getConfigManager().getCategoryItem(category);

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            List<String> itemLore = this.lore;
            if (itemLore.isEmpty()) itemLore.add("&7&o- " + makeFriendly(category) + " reward");
            itemLore = ChatColorHandler.translateAlternateColorCodes(itemLore);
            itemMeta.setLore(itemLore);
            item.setItemMeta(itemMeta);
        }

        return item;
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable List<String> lore, @Nullable Sound sound) {
        return new DailyRewardCollection(rewards, priority, category, lore, sound);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category, @Nullable List<String> lore) {
        return new DailyRewardCollection(rewards, priority, category, lore, null);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority, @Nullable String category) {
        return new DailyRewardCollection(rewards, priority, category, null, null);
    }

    public static DailyRewardCollection from(@Nullable Collection<Reward> rewards, int priority) {
        return new DailyRewardCollection(rewards, priority, null, null, null);
    }

    public static DailyRewardCollection empty() {
        return new DailyRewardCollection(null, 0, null, null, null);
    }
}
