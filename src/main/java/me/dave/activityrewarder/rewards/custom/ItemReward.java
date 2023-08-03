package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class ItemReward implements Reward {
    private final ItemStack itemStack;

    public ItemReward(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemReward(ConfigurationSection configurationSection) {

        ItemStack item = ConfigParser.getItem(configurationSection.getString("material").toUpperCase(), Material.GOLD_NUGGET);
        item.setAmount(configurationSection.getInt("amount", 1));

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
