package org.lushplugins.lushrewards.module;

import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushlib.utils.converter.YamlConverter;
import org.lushplugins.lushrewards.LushRewards;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.lushplugins.lushrewards.user.RewardUser;

import java.util.concurrent.ConcurrentHashMap;

public abstract class RewardModule {
    protected final String id;
    protected boolean shouldNotify = false;
    protected final ConcurrentHashMap<String, DisplayItemStack> itemTemplates = new ConcurrentHashMap<>();

    public RewardModule(String id, ConfigurationSection config) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public abstract boolean requiresPlaytimeTracker();

    public abstract boolean hasClaimableRewards(Player player, RewardUser user);

    public boolean hasClaimableRewards(Player player) {
        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
        return hasClaimableRewards(player, user);
    }

    @SuppressWarnings("UnusedReturnValue")
    public abstract boolean claimRewards(Player player, RewardUser user);



    public boolean shouldNotify() {
        return shouldNotify;
    }

    public void setShouldNotify(boolean shouldNotify) {
        this.shouldNotify = shouldNotify;
    }

    public DisplayItemStack getItemTemplate(String key) {
        DisplayItemStack itemTemplate = itemTemplates.get(key);
        return itemTemplate != null ? itemTemplate : DisplayItemStack.empty();
    }

    public void reloadItemTemplates(ConfigurationSection itemTemplatesSection) {
        // Clears category map
        itemTemplates.clear();

        // Checks if categories section exists
        if (itemTemplatesSection == null) {
            return;
        }

        // Repopulates category map
        itemTemplatesSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection categorySection) {
                itemTemplates.put(categorySection.getName(), YamlConverter.getDisplayItem(categorySection));
                LushRewards.getInstance().getLogger().info("Loaded item-template: " + categorySection.getName());
            }
        });
    }

    public static class Type {
        public static final String PLAYTIME_TRACKER = "playtime-tracker";
    }
}
