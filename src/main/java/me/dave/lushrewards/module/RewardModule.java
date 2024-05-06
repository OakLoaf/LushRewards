package me.dave.lushrewards.module;

import me.dave.lushrewards.LushRewards;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushlib.module.Module;
import org.bukkit.entity.Player;
import org.lushplugins.lushlib.utils.SimpleItemStack;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public abstract class RewardModule extends Module {
    protected final File moduleFile;
    private final boolean requiresTimeTracker;
    private final ConcurrentHashMap<String, SimpleItemStack> itemTemplates = new ConcurrentHashMap<>();

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

    public SimpleItemStack getItemTemplate(String key) {
        SimpleItemStack itemTemplate = itemTemplates.get(key);
        return itemTemplate != null ? itemTemplate.clone() : new SimpleItemStack();
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
                itemTemplates.put(categorySection.getName(), SimpleItemStack.from(categorySection));
                LushRewards.getInstance().getLogger().info("Loaded item-template: " + categorySection.getName());
            }
        });
    }

    @FunctionalInterface
    public interface Constructor<T extends RewardModule> {
        T build(String id, File file) ;
    }

    public static class Type {
        public static final String DAILY_REWARDS = "daily-rewards";
        public static final String PLAYTIME_REWARDS = "playtime-rewards";
        public static final String PLAYTIME_TRACKER = "playtime-tracker";
    }
}
