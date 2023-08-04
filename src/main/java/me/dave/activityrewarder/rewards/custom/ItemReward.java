package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemReward implements Reward {
    private final ItemStack itemStack;

    public ItemReward(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemReward(Map<?, ?> map) {
        String material = map.containsKey("material") ? (String) map.get("material") : "e";
        int amount = map.containsKey("amount") ? (int) map.get("amount") : 1;

        ItemStack item = ConfigParser.getItem(material.toUpperCase(), Material.GOLD_NUGGET);
        item.setAmount(amount);

        this.itemStack = item;
    }

    @Override
    public void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(itemStack);
        for (ItemStack item: droppedItems.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
