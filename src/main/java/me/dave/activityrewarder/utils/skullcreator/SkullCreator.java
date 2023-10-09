package me.dave.activityrewarder.utils.skullcreator;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public interface SkullCreator {
    ItemStack getCustomSkull(String texture);

    void mutateItemMeta(SkullMeta meta, String b64);

    String getB64(ItemStack itemStack);

    String getTexture(Player player);
}
