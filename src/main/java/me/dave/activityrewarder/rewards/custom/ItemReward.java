package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ItemReward implements Reward {
    private final SimpleItemStack itemStack;

    public ItemReward(SimpleItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemReward(@NotNull Map<?, ?> map) {
        Material material = ConfigParser.getMaterial((String) map.get("material"));

        if (material != null) {
            this.itemStack = new SimpleItemStack(material);

            try {
                if (map.containsKey("amount")) {
                    itemStack.setAmount((int) map.get("amount"));
                }
                if (map.containsKey("display-name")) {
                    itemStack.setDisplayName((String) map.get("display-name"));
                }
                if (map.containsKey("custom-model-data")) {
                    itemStack.setCustomModelData((int) map.get("custom-model-data"));
                }
                if (map.containsKey("enchanted")) {
                    itemStack.setEnchanted((boolean) map.get("enchanted"));
                }
            } catch(ClassCastException exc) {
                throw new InvalidRewardException("Invalid config format at '" + map + "', could not parse data");
            }
        }
        else {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }
    }

    @Override
    public void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(itemStack.getItemStack(player));
        droppedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
    }
}
