package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemReward implements Reward {
    private final SimpleItemStack simpleItemStack;

    public ItemReward(Map<?, ?> map) throws InvalidRewardException {
        Map<?, ?> itemMap;
        try {
            itemMap = map.containsKey("item") ? (Map<?, ?>) map.get("item") : Collections.emptyMap();
        } catch(ClassCastException exc) {
            ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "'");
            throw new InvalidRewardException();
        }

        Material material = ConfigParser.getMaterial((String) itemMap.get("material"));

        if (material != null) {
            this.simpleItemStack = new SimpleItemStack(material);

            try {
                if (itemMap.containsKey("amount")) simpleItemStack.setAmount((int) itemMap.get("amount"));
                if (itemMap.containsKey("display-name")) simpleItemStack.setDisplayName((String) itemMap.get("display-name"));
                if (itemMap.containsKey("custom-model-data")) simpleItemStack.setCustomModelData((int) itemMap.get("custom-model-data"));
                if (itemMap.containsKey("enchanted")) simpleItemStack.setEnchanted((boolean) itemMap.get("enchanted"));
            } catch(ClassCastException exc) {
                ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + map + "'");
                throw new InvalidRewardException();
            }
        }
        else {
            ActivityRewarder.getInstance().getLogger().severe("Invalid config format at '" + itemMap + "'");
            throw new InvalidRewardException();
        }
    }

    @Override
    public void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(simpleItemStack.getItemStack());
        for (ItemStack item: droppedItems.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
