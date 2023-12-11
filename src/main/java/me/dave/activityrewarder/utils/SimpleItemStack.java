package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.exceptions.InvalidRewardException;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SimpleItemStack implements Cloneable {
    private Material material = null;
    private int amount = 1;
    private String displayName = null;
    private List<String> lore = null;
    private Boolean enchanted = null;
    private int customModelData = 0;
    private String skullTexture = null;

    public SimpleItemStack() {}

    public SimpleItemStack(@Nullable Material material) {
        this.material = material;
    }

    public SimpleItemStack(@Nullable Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    @Nullable
    public Material getType() {
        return material;
    }

    public boolean hasType() {
        return material != null;
    }

    public void setType(@Nullable Material material) {
        this.material = material;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public boolean hasDisplayName() {
        return displayName != null;
    }

    public void setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    public boolean hasLore() {
        return lore != null;
    }

    public void setLore(@Nullable List<String> lore) {
        this.lore = lore;
    }

    @Nullable
    public Boolean getEnchanted() {
        return enchanted;
    }

    public boolean hasEnchant() {
        return enchanted != null;
    }

    public void setEnchanted(@Nullable Boolean enchanted) {
        this.enchanted = enchanted;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasCustomModelData() {
        return customModelData != 0;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public String getSkullTexture() {
        return skullTexture;
    }

    public boolean hasSkullTexture() {
        return skullTexture != null;
    }

    public void setSkullTexture(@Nullable String texture) {
        this.skullTexture = texture;
    }

    public boolean isBlank() {
        return
            material == null
                && amount == 1
                && displayName == null
                && lore == null
                && enchanted == null
                && customModelData == 0
                && skullTexture == null;
    }

    public void parseColors(Player player) {
        if (hasDisplayName()) {
            displayName = ChatColorHandler.translate(displayName, player);
        }
        if (hasLore()) {
            lore = lore.stream().map(line -> ChatColorHandler.translate(line, player)).toList();
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
            if (enchanted != null && enchanted) {
                itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
                itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            if (customModelData != 0) {
                itemMeta.setCustomModelData(customModelData);
            }
            if (itemMeta instanceof SkullMeta skullMeta && skullTexture != null) {
                try {
                    if (skullTexture.equals("mirror") && player != null) {
                        String playerB64 = ActivityRewarder.getSkullCreator().getTexture(player);
                        if (playerB64 != null) {
                            ActivityRewarder.getSkullCreator().mutateItemMeta(skullMeta, playerB64);
                        }
                    } else {
                        ActivityRewarder.getSkullCreator().mutateItemMeta(skullMeta, skullTexture);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            itemStack.setItemMeta(itemMeta);
        }

        return itemStack;
    }

    public void save(ConfigurationSection configurationSection) {
        if (material != null) {
            configurationSection.set("material", material.name());
        }
        if (amount != 1) {
            configurationSection.set("amount", amount);
        }
        if (displayName != null) {
            configurationSection.set("display-name", displayName);
        }
        if (lore != null) {
            configurationSection.set("lore", lore);
        }
        if (enchanted != null) {
            configurationSection.set("enchanted", enchanted);
        }
        if (customModelData != 0) {
            configurationSection.set("custom-model-data", customModelData);
        }
        if (skullTexture != null) {
            configurationSection.set("skull-texture", skullTexture);
        }
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();

        if (material != null) {
            map.put("material", material.name());
        }
        if (amount != 1) {
            map.put("amount", amount);
        }
        if (displayName != null) {
            map.put("display-name", displayName);
        }
        if (lore != null) {
            map.put("lore", lore);
        }
        if (enchanted != null) {
            map.put("enchanted", enchanted);
        }
        if (customModelData != 0) {
            map.put("custom-model-data", customModelData);
        }
        if (skullTexture != null) {
            map.put("skull-texture", skullTexture);
        }

        return map;
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
        result.setEnchanted(overwrite.hasEnchant() ? overwrite.getEnchanted() : original.getEnchanted());
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
                simpleItemStack.setSkullTexture(ActivityRewarder.getSkullCreator().getB64(itemStack));
            }
        }
        return simpleItemStack;
    }

    public static SimpleItemStack from(@NotNull ConfigurationSection configurationSection) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        if (configurationSection.contains("material")) {
            simpleItemStack.setType(ConfigParser.getMaterial(configurationSection.getString("material", null)));
        }
        if (configurationSection.contains("amount")) {
            simpleItemStack.setAmount(configurationSection.getInt("amount", 1));
        }
        if (configurationSection.contains("display-name")) {
            simpleItemStack.setDisplayName(configurationSection.getString("display-name", null));
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
            simpleItemStack.setSkullTexture(configurationSection.getString("skull-texture", null));
        }

        return simpleItemStack;
    }

    @SuppressWarnings("unchecked")
    public static SimpleItemStack from(@NotNull Map<?, ?> configurationMap) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        try {
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
        } catch(ClassCastException exc) {
            throw new InvalidRewardException("Invalid format at '" + configurationMap + "', could not parse data");
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
