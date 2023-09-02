package me.dave.activityrewarder.rewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemReward implements Reward {
    private final ItemStack itemStack;

    public ItemReward(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void giveReward(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(itemStack);
        droppedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
    }
}
