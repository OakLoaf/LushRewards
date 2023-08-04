package me.dave.activityrewarder.rewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record DailyRewardCollection(int priority, String category, List<String> lore, Sound sound, List<Reward> rewards) {

    public DailyRewardCollection(int priority, @Nullable String category, @Nullable List<String> lore, @Nullable Sound sound, @Nullable List<Reward> rewards) {
        this.priority = priority;
        this.category = category != null ? category : ActivityRewarder.getRewardManager().getDefaultReward().category();
        this.lore = lore != null ? lore : ActivityRewarder.getRewardManager().getDefaultReward().lore();
        this.sound = sound != null ? sound : ActivityRewarder.getRewardManager().getDefaultReward().sound();
        this.rewards = rewards != null ? rewards : ActivityRewarder.getRewardManager().getDefaultReward().rewards();
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
        String category = this.category();
        ItemStack item = ActivityRewarder.getConfigManager().getCategoryItem(category);

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta != null) {
            List<String> itemLore = this.lore();
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
}
