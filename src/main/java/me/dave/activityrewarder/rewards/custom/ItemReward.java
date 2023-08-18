package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
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
    private final SimpleItemStack simpleItemStack;

    public ItemReward(@NotNull Map<?, ?> map) throws InvalidRewardException {
        Material material = ConfigParser.getMaterial((String) map.get("material"));

        if (material != null) {
            this.simpleItemStack = new SimpleItemStack(material);

            try {
                if (map.containsKey("amount")) {
                    simpleItemStack.setAmount((int) map.get("amount"));
                }
                if (map.containsKey("display-name")) {
                    simpleItemStack.setDisplayName((String) map.get("display-name"));
                }
                if (map.containsKey("custom-model-data")) {
                    simpleItemStack.setCustomModelData((int) map.get("custom-model-data"));
                }
                if (map.containsKey("enchanted")) {
                    simpleItemStack.setEnchanted((boolean) map.get("enchanted"));
                }
            } catch(ClassCastException exc) {
                ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "', could not parse data");
                throw new InvalidRewardException();
            }
        }
        else {
            ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "'");
            throw new InvalidRewardException();
        }
    }

    @Override
    public void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(simpleItemStack.getItemStack(player));
        droppedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
    }
}
