package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.exceptions.InvalidRewardException;
import me.dave.activityrewarder.utils.SchedulerType;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ItemReward extends Reward {
    private final SimpleItemStack itemStack;

    public ItemReward(SimpleItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemReward(@NotNull Map<?, ?> map) {
        SimpleItemStack itemStack = SimpleItemStack.from(map);

        if (itemStack.getType() != null) {
            this.itemStack = itemStack;
        }
        else {
            throw new InvalidRewardException("Invalid config format at '" + map + "'");
        }
    }

    @Override
    protected void giveTo(Player player) {
        HashMap<Integer, ItemStack> droppedItems = player.getInventory().addItem(itemStack.getItemStack(player));
        droppedItems.values().forEach(item -> player.getWorld().dropItem(player.getLocation(), item));
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = itemStack.asMap();

        rewardMap.put("type", "item");

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.PLAYER;
    }
}
