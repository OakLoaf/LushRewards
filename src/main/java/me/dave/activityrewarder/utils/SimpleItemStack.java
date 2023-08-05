package me.dave.activityrewarder.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SimpleItemStack {
    private Material material;
    private int amount = 1;
    private String displayName = null;
    private List<String> lore = null;
    private int customModelData = 0;
    private boolean enchanted = false;

    public SimpleItemStack(@NotNull Material material) {
        this.material = material;
    }

    public SimpleItemStack(@NotNull Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public SimpleItemStack(@NotNull ItemStack itemStack) {
        this.material = itemStack.getType();
        this.amount = itemStack.getAmount();

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (itemMeta.hasDisplayName()) this.displayName = itemMeta.getDisplayName();
            if (itemMeta.hasLore()) this.lore = itemMeta.getLore();
            if (itemMeta.hasCustomModelData()) this.customModelData = itemMeta.getCustomModelData();
            if (itemMeta.hasEnchants()) this.enchanted = true;
        }
    }

    public SimpleItemStack(@NotNull ConfigurationSection configurationSection) {
        this.material = ConfigParser.getMaterial(configurationSection.getString("material"));
        this.amount = configurationSection.getInt("amount", 1);
        this.displayName = configurationSection.getString("display-name");
        this.lore = configurationSection.getStringList("lore");
        this.customModelData = configurationSection.getInt("custom-model-data");
        this.enchanted = configurationSection.getBoolean("enchanted");
    }

    public void setType(@NotNull Material material) {
        this.material = material;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    public void setLore(@Nullable List<String> lore) {
        this.lore = lore;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public void setEnchanted(boolean enchanted) {
        this.enchanted = enchanted;
    }

    public ItemStack getItemStack() {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            if (displayName != null) itemMeta.setDisplayName(displayName);
            if (lore != null) itemMeta.setLore(lore);
            if (customModelData != 0) itemMeta.setCustomModelData(customModelData);
            if (enchanted) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }
}
