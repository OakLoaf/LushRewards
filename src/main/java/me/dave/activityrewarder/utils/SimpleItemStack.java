package me.dave.activityrewarder.utils;

import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SimpleItemStack implements Cloneable {
    private Material material = null;
    private int amount = 1;
    private String displayName = null;
    private List<String> lore = null;
    private Boolean enchanted = false;
    private int customModelData = 0;
    private String skullTexture = null;

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

    public boolean getEnchanted() {
        return enchanted;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public String getSkullTexture() {
        return skullTexture;
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

    public boolean hasEnchantDefined() {
        return enchanted != null;
    }

    public boolean hasCustomModelData() {
        return customModelData != 0;
    }

    public boolean hasSkullTexture() {
        return skullTexture != null;
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

    public void setEnchanted(@Nullable Boolean enchanted) {
        this.enchanted = enchanted;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public void setSkullTexture(@Nullable String texture) {
        this.skullTexture = texture;
    }

    public void parseColors(Player player) {
        if (hasDisplayName()) {
            displayName = ChatColorHandler.translateAlternateColorCodes(displayName, player);
        }
        if (hasLore()) {
            lore = ChatColorHandler.translateAlternateColorCodes(lore, player);
        }
    }

    public ItemStack getItemStack() {
        return getItemStack(null);
    }

    public ItemStack getItemStack(@Nullable Player player) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            if (displayName != null) {
                itemMeta.setDisplayName(displayName);
            }
            if (lore != null) {
                itemMeta.setLore(lore);
            }
            if (enchanted) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (customModelData != 0) {
                itemMeta.setCustomModelData(customModelData);
            }
            if (itemMeta instanceof SkullMeta skullMeta && skullTexture != null) {
                if (skullTexture.equals("mirror") && player != null) {
                    SkullCreator.mutateItemMeta(skullMeta, SkullCreator.getTexture(player));
                } else {
                    SkullCreator.mutateItemMeta(skullMeta, skullTexture);
                }
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
        result.setEnchanted(overwrite.hasEnchantDefined() ? overwrite.getEnchanted() : original.getEnchanted());
        result.setSkullTexture(overwrite.hasSkullTexture() ? overwrite.getSkullTexture() : original.getSkullTexture());

        return result;
    }

    public static SimpleItemStack overwrite(@NotNull SimpleItemStack original, @NotNull SimpleItemStack... overwrites) {
        SimpleItemStack result = original;

        for (SimpleItemStack overwrite : overwrites) {
            result = overwrite(result, overwrite);
        }

        return result;
    }

    public static SimpleItemStack from(@NotNull ItemStack itemStack) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();
        simpleItemStack.setType(itemStack.getType());
        simpleItemStack.setAmount(itemStack.getAmount());

        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            if (itemMeta.hasDisplayName()) {
                simpleItemStack.setDisplayName(itemMeta.getDisplayName());
            }
            if (itemMeta.hasLore()) {
                simpleItemStack.setLore(itemMeta.getLore());
            }
            if (itemMeta.hasEnchants()) {
                simpleItemStack.setEnchanted(true);
            }
            if (itemMeta.hasCustomModelData()) {
                simpleItemStack.setCustomModelData(itemMeta.getCustomModelData());
            }
            if (itemMeta instanceof SkullMeta) {
                simpleItemStack.setSkullTexture(SkullCreator.getB64(itemStack));
            }
        }
        return simpleItemStack;
    }

    public static SimpleItemStack from(@NotNull ConfigurationSection configurationSection) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        if (configurationSection.contains("material")) {
            simpleItemStack.setType(ConfigParser.getMaterial(configurationSection.getString("material")));
        }
        if (configurationSection.contains("amount")) {
            simpleItemStack.setAmount(configurationSection.getInt("amount", 1));
        }
        if (configurationSection.contains("display-name")) {
            simpleItemStack.setDisplayName(configurationSection.getString("display-name"));
        }
        if (configurationSection.contains("lore")) {
            simpleItemStack.setLore(configurationSection.getStringList("lore"));
        }
        if (configurationSection.contains("enchanted")) {
            simpleItemStack.setEnchanted(configurationSection.getBoolean("enchanted", false));
        }
        if (configurationSection.contains("custom-model-data")) {
            simpleItemStack.setCustomModelData(configurationSection.getInt("custom-model-data"));
        }
        if (configurationSection.contains("skull-texture")) {
            simpleItemStack.setSkullTexture(configurationSection.getString("skull-texture"));
        }

        return simpleItemStack;
    }

    @SuppressWarnings("unchecked")
    public static SimpleItemStack from(@NotNull Map<?, ?> configurationMap) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        if (configurationMap.containsKey("material")) {
            simpleItemStack.setType(ConfigParser.getMaterial((String) configurationMap.get("material")));
        }
        if (configurationMap.containsKey("amount")) {
            simpleItemStack.setAmount((int) configurationMap.get("amount"));
        }
        if (configurationMap.containsKey("display-name")) {
            simpleItemStack.setDisplayName((String) configurationMap.get("display-name"));
        }
        if (configurationMap.containsKey("lore")) {
            simpleItemStack.setLore((List<String>) configurationMap.get("lore"));
        }
        if (configurationMap.containsKey("enchanted")) {
            simpleItemStack.setEnchanted((boolean) configurationMap.get("enchanted"));
        }
        if (configurationMap.containsKey("custom-model-data")) {
            simpleItemStack.setCustomModelData((int) configurationMap.get("custom-model-data"));
        }
        if (configurationMap.containsKey("skull-texture")) {
            simpleItemStack.setSkullTexture((String) configurationMap.get("skull-texture"));
        }

        return simpleItemStack;
    }

    @Override
    public SimpleItemStack clone() {
        try {
            return (SimpleItemStack) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
