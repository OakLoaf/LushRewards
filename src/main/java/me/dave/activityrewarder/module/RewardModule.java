package me.dave.activityrewarder.module;

import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@SuppressWarnings("unused")
public abstract class RewardModule extends Module {
    protected final File moduleFile;
    private final Class<? extends ModuleData> dataClass;
    private final boolean requiresTimeTracker;

    public RewardModule(String id, File moduleFile) {
        super(id);
        this.moduleFile = moduleFile;
        this.requiresTimeTracker = false;
        this.dataClass = null;
    }

    public RewardModule(String id, File moduleFile, @Nullable Class<? extends ModuleData> dataClass) {
        super(id);
        this.moduleFile = moduleFile;
        this.requiresTimeTracker = false;
        this.dataClass = dataClass;
    }

    public RewardModule(String id, File moduleFile, @Nullable Class<? extends ModuleData> dataClass, boolean requiresTimeTracker) {
        super(id);
        this.moduleFile = moduleFile;
        this.requiresTimeTracker = requiresTimeTracker;
        this.dataClass = dataClass;
    }

    public abstract boolean claimRewards(Player player);

    public File getModuleFile() {
        return moduleFile;
    }

    public Class<? extends ModuleData> getDataClass() {
        return dataClass;
    }

    public boolean requiresPlaytimeTracker() {
        return requiresTimeTracker;
    }



    @FunctionalInterface
    public interface CallableRewardModule<V extends RewardModule> {
        V call(String id, File file) ;
    }
}
