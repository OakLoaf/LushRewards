package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemReward implements Reward {
    private final ItemStack item;
    private final String size;

    public ItemReward(Material material, int count, String size) {
        this.item = new ItemStack(material, count);
        this.size = size;
    }

    @Override
    public String getSize() {
        return size;
    }


    @Override
    public void giveReward(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(item);
        for (ItemStack item: droppedItems.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
