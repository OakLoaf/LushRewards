package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemReward extends Reward {
    private final ItemStack itemStack;

    public ItemReward(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void giveReward(Player player, int hourlyAmount) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(itemStack);
        for (ItemStack item: droppedItems.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
