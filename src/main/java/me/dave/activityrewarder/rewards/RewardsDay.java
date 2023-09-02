package me.dave.activityrewarder.rewards;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RewardsDay {
    private final String size;
    private final List<String> lore;
    private final ArrayList<Reward> rewards;

    public RewardsDay(String size, List<String> lore, ArrayList<Reward> rewards) {
        this.size = size;
        this.lore = lore;
        this.rewards = rewards;
    }

    public String getSize() {
        return size;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getRewardCount() {
        return rewards.size();
    }

    public void giveRewards(Player player) {
        for (Reward reward : rewards) {
            try {
                reward.giveReward(player);
            } catch(Exception e) {
                ActivityRewarder.getInstance().getLogger().severe("Error occurred when giving reward (" +reward.toString() + ") to " + player.getName());
                e.printStackTrace();
            }
        }
    }

    public void giveRewards(Player player, int multiplier) {
        for (int i = 0; i < multiplier; i++) {
            giveRewards(player);
        }
    }

    public ItemStack asItem() {
        // Get the current reward's size item
        String size = this.getSize();
        ItemStack rewardItem = ActivityRewarder.configManager.getSizeItem(size);
        ItemMeta rewardItemMeta = rewardItem.getItemMeta();
        List<String> itemLore = this.getLore();
        if (itemLore.isEmpty()) {
            itemLore.add("&7&o- " + makeFriendly(size) + " reward");
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
