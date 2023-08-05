package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.rewards.Reward;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemReward implements Reward {
    private final SimpleItemStack simpleItemStack;

    public ItemReward(ItemStack itemStack) {
        this.simpleItemStack = new SimpleItemStack(itemStack);
    }

    public ItemReward(Map<?, ?> map) {
        ConfigurationSection configurationSection;
        try {
            configurationSection = (ConfigurationSection) map.get("items");
        } catch(ClassCastException exc) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "'");
            this.simpleItemStack = new SimpleItemStack(Material.AIR);
            return;
        }

        this.simpleItemStack = new SimpleItemStack(configurationSection);
    }

    @Override
    public void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(simpleItemStack.getItemStack());
        for (ItemStack item: droppedItems.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
