package me.dave.lushrewards.module;

import org.lushplugins.lushlib.module.Module;
import org.bukkit.entity.Player;

import java.io.File;

@SuppressWarnings("unused")
public abstract class RewardModule extends Module {
    protected final File moduleFile;
    private final boolean requiresTimeTracker;

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
