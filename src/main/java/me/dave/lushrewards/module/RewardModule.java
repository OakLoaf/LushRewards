package me.dave.lushrewards.module;

import me.dave.lushrewards.LushRewards;
import me.dave.platyutils.module.Module;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public abstract class RewardModule<T extends RewardModule.UserData> extends Module {
    protected final File moduleFile;
    protected final ConcurrentHashMap<UUID, T> userDataMap = new ConcurrentHashMap<>();
    private final Class<T> userDataClass;
    private final UserData.Constructor<T> userDataConstructor;
    private final boolean requiresTimeTracker;

    public RewardModule(String id, File moduleFile, Class<T> userDataClass) {
        super(id);
        this.moduleFile = moduleFile;
        this.userDataClass = userDataClass;
        this.userDataConstructor = null;
        this.requiresTimeTracker = false;
    }

    public RewardModule(String id, File moduleFile, Class<T> userDataClass, @Nullable UserData.Constructor<T> userDataConstructor) {
        super(id);
        this.moduleFile = moduleFile;
        this.userDataClass = userDataClass;
        this.userDataConstructor = userDataConstructor;
        this.requiresTimeTracker = false;
    }

    public RewardModule(String id, File moduleFile, Class<T> userDataClass, @Nullable UserData.Constructor<T> userDataConstructor, boolean requiresTimeTracker) {
        super(id);
        this.moduleFile = moduleFile;
        this.userDataClass = userDataClass;
        this.userDataConstructor = userDataConstructor;
        this.requiresTimeTracker = requiresTimeTracker;
    }

    @SuppressWarnings("UnusedReturnValue")
    public abstract boolean claimRewards(Player player);

    public File getModuleFile() {
        return moduleFile;
    }

    public UserData.Constructor<? extends UserData> getUserDataConstructor() {
        return userDataConstructor;
    }

    public boolean requiresPlaytimeTracker() {
        return requiresTimeTracker;
    }

    public Class<T> getUserDataClass() {
        return userDataClass;
    }

    public T getUserData(UUID uuid) {
        return userDataMap.get(uuid);
    }

    public void loadUserData(UUID uuid, UserData userData) {
        try {
            if (userDataClass.isInstance(userData)) {
                this.userDataMap.put(uuid, userDataClass.cast(userData));
            }
        } catch (ClassCastException e) {
            LushRewards.getInstance().getLogger().severe(e.getMessage());
            e.printStackTrace();
        }
    }

    public void unloadUserData(UUID uuid) {
        userDataMap.remove(uuid);
    }

    @FunctionalInterface
    public interface Constructor<V extends RewardModule<UserData>> {
        V build(String id, File file) ;
    }

    public static abstract class UserData {
        protected final String id;

        public UserData(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @FunctionalInterface
        public interface Constructor<V extends UserData> {
            V build();
        }
    }
}
