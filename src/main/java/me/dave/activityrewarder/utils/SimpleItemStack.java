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

public class SimpleItemStack implements Cloneable {
    private Material material = null;
    private int amount = 1;
    private String displayName = null;
    private List<String> lore = null;
    private int customModelData = 0;
    private boolean enchanted = false;

    public SimpleItemStack() {}

    public SimpleItemStack(@NotNull Material material) {
        this.material = material;
    }

    public SimpleItemStack(@NotNull Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Nullable
    public Material getType() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean getEnchanted() {
        return enchanted;
    }

    public boolean hasType() {
        return material != null;
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public boolean hasLore() {
        return lore != null;
    }

    public boolean hasCustomModelData() {
        return customModelData != 0;
    }

    public void setType(@Nullable Material material) {
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

    public static SimpleItemStack overwrite(@NotNull SimpleItemStack original, @NotNull SimpleItemStack overwrite) {
        SimpleItemStack result = new SimpleItemStack();

        if (overwrite.hasType()) {
            result.setType(overwrite.getType());
            result.setCustomModelData(overwrite.getCustomModelData());
        }
        else {
            result.setType(original.getType());
            result.setCustomModelData(original.getCustomModelData());
        }

        result.setAmount(overwrite.getAmount() != 1 ? overwrite.getAmount() : original.getAmount());
        result.setDisplayName(overwrite.hasDisplayName() ? overwrite.getDisplayName() : original.getDisplayName());
        result.setLore(overwrite.hasLore() ? overwrite.getLore() : original.getLore());
        // TODO: Add way to check if overridable
        result.setEnchanted(overwrite.getEnchanted());

        return result;
    }

    public static SimpleItemStack from(@NotNull ItemStack itemStack) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();
        simpleItemStack.setType(itemStack.getType());
        simpleItemStack.setAmount(itemStack.getAmount());

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (itemMeta.hasDisplayName()) simpleItemStack.setDisplayName(itemMeta.getDisplayName());
            if (itemMeta.hasLore()) simpleItemStack.setLore(itemMeta.getLore());
            if (itemMeta.hasCustomModelData()) simpleItemStack.setCustomModelData(itemMeta.getCustomModelData());
            if (itemMeta.hasEnchants()) simpleItemStack.setEnchanted(true);
        }
        return simpleItemStack;
    }

    public static SimpleItemStack from(@NotNull ConfigurationSection configurationSection) {
        return from(configurationSection, null);
    }

    public static SimpleItemStack from(@NotNull ConfigurationSection configurationSection, Material def) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();
        if (configurationSection.contains("material")) simpleItemStack.setType(ConfigParser.getMaterial(configurationSection.getString("material"), def));
        if (configurationSection.contains("amount")) simpleItemStack.setAmount(configurationSection.getInt("amount", 1));
        if (configurationSection.contains("display-name")) simpleItemStack.setDisplayName(configurationSection.getString("display-name"));
        if (configurationSection.contains("lore")) simpleItemStack.setLore(configurationSection.getStringList("lore"));
        if (configurationSection.contains("custom-model-data")) simpleItemStack.setCustomModelData(configurationSection.getInt("custom-model-data"));
        if (configurationSection.contains("enchanted")) simpleItemStack.setEnchanted(configurationSection.getBoolean("enchanted", false));
        return simpleItemStack;
    }

    @Override
    public SimpleItemStack clone() {
        try {
            SimpleItemStack clone = (SimpleItemStack) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
