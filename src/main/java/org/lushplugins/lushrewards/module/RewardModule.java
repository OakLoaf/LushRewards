package org.lushplugins.lushrewards.module;

import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushlib.utils.converter.YamlConverter;
import org.lushplugins.lushrewards.LushRewards;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushlib.module.Module;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public abstract class RewardModule extends Module {
    protected final File moduleFile;
    private final boolean requiresTimeTracker;
    private boolean shouldNotify = false;
    private final ConcurrentHashMap<String, DisplayItemStack> itemTemplates = new ConcurrentHashMap<>();

    public RewardModule(String id, File moduleFile) {
        super(id);
        this.moduleFile = moduleFile;
        this.requiresTimeTracker = false;
    }

    public RewardModule(String id, File moduleFile, boolean requiresTimeTracker) {
        super(id);
        this.moduleFile = moduleFile;
        this.requiresTimeTracker = requiresTimeTracker;
    }

    public abstract boolean hasClaimableRewards(Player player);

    @SuppressWarnings("UnusedReturnValue")
    public abstract boolean claimRewards(Player player);

    public File getModuleFile() {
        return moduleFile;
    }

    public boolean requiresPlaytimeTracker() {
        return requiresTimeTracker;
    }

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
        public static final String DAILY_REWARDS = "daily-rewards";
        public static final String PLAYTIME_REWARDS = "playtime-rewards";
        public static final String PLAYTIME_TRACKER = "playtime-tracker";
    }
}
