package me.dave.activityrewarder.rewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RewardCollection {
    private final int priority;
    private final Sound sound;
    private final String category;
    private final List<String> lore;
    private final List<Reward> rewards;

    public RewardCollection(int priority, @Nullable Sound sound, @Nullable String category, @Nullable List<String> lore, @Nullable ArrayList<Reward> rewards) {
        this.priority = priority;
        this.sound = sound != null ? sound : ActivityRewarder.configManager.getDefaultReward().getSound();
        this.category = category != null ? category : ActivityRewarder.configManager.getDefaultReward().getCategory();
        this.lore = lore != null ? lore : ActivityRewarder.configManager.getDefaultReward().getLore();
        this.rewards = rewards != null ? rewards : ActivityRewarder.configManager.getDefaultReward().getRewards();
    }

    public int getPriority() {
        return priority;
    }

    public Sound getSound() {
        return sound;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public void giveRewards(Player player) {
        for (Reward reward : rewards) {
            reward.giveTo(player);
        }
    }

    public ItemStack asItem() {
        // Get the current reward's category item
        String category = this.getCategory();
        ItemStack rewardItem = ActivityRewarder.configManager.getCategoryItem(category);
        ItemMeta rewardItemMeta = rewardItem.getItemMeta();
        List<String> itemLore = this.getLore();
        if (itemLore.isEmpty()) {
            itemLore.add("&7&o- " + makeFriendly(category) + " reward");
        }
        itemLore = ChatColorHandler.translateAlternateColorCodes(itemLore);
        rewardItemMeta.setLore(itemLore);
        rewardItem.setItemMeta(rewardItemMeta);

        return rewardItem;
    }

    private String makeFriendly(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();
    }
}
