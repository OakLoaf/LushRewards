package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ConfigParser {

    @Nullable
    public static Material getMaterial(String materialName) {
        return getMaterial(materialName, null);
    }

    public static Material getMaterial(String materialName, Material def) {
        if (materialName == null) return def;

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException err) {
            ActivityRewarder.getInstance().getLogger().warning("Ignoring " + materialName + ", that is not a valid material.");
            if (def != null) {
                material = def;
                ActivityRewarder.getInstance().getLogger().warning("Defaulted material to " + def.name() + ".");
            }
            else return null;
        }
        return material;
    }

    @Nullable
    public static ItemStack getItem(String materialName, Material def) {
        Material material = getMaterial(materialName, def);
        if (material == null) return null;
        return new ItemStack(material);
    }

    @NotNull
    public static ItemStack getItem(ConfigurationSection configurationSection) {
        return getItem(configurationSection, Material.STONE);
    }

    @NotNull
    public static ItemStack getItem(ConfigurationSection configurationSection, @NotNull Material def) {
        Material material = configurationSection != null ? getMaterial(configurationSection.getString("material"), def) : def;
        if (material == null) material = def;

        ItemStack item = new ItemStack(material);

        if (configurationSection != null) {
            item.setAmount(configurationSection.getInt("amount", 1));
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta != null) {
                if (configurationSection.getStringList("lore").isEmpty()) itemMeta.setLore(configurationSection.getStringList("lore"));
                if (configurationSection.getInt("custom-model-data") != 0) itemMeta.setCustomModelData(configurationSection.getInt("custom-model-data"));
                item.setItemMeta(itemMeta);
            }
        }


        return item;
    }

    @Nullable
    public static Sound getSound(String soundName) {
        return getSound(soundName, null);
    }

    public static Sound getSound(String soundName, Sound def) {
        Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException err) {
            ActivityRewarder.getInstance().getLogger().warning("Ignoring " + soundName + ", that is not a valid sound.");
            if (def != null) {
                sound = def;
                ActivityRewarder.getInstance().getLogger().warning("Defaulted sound to " + def.name() + ".");
            }
            else return null;
        }
        return sound;
    }
}
