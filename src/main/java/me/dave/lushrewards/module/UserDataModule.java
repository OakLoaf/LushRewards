package me.dave.lushrewards.module;

import com.google.gson.JsonObject;
import me.dave.lushrewards.LushRewards;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface UserDataModule<T extends UserDataModule.UserData> {

    String getId();

    T getUserData(UUID uuid);

    default CompletableFuture<T> getOrLoadUserData(UUID uuid, boolean cacheUser) {
        return LushRewards.getInstance().getDataManager().getOrLoadUserData(uuid, this, cacheUser);
    }

    default CompletableFuture<T> loadUserData(UUID uuid, boolean cacheUser) {
        return LushRewards.getInstance().getDataManager().loadUserData(uuid, this, cacheUser);
    }

    default CompletableFuture<Boolean> saveUserData(@NotNull UUID uuid, T userData) {
        return LushRewards.getInstance().getDataManager().saveUserData(uuid, userData);
    }

    void cacheUserData(UUID uuid, UserData userData);

    void uncacheUserData(UUID uuid);

    T getDefaultData(UUID uuid);

    Class<T> getUserDataClass();

    abstract class UserData {
        private final UUID uuid;
        private final String moduleId;

        public UserData(UUID uuid, String moduleId) {
            this.uuid = uuid;
            this.moduleId = moduleId;
        }

        public @NotNull UUID getUniqueId() {
            return uuid;
        }

        public String getModuleId() {
            return moduleId;
        }

        public JsonObject asJson() {
            return LushRewards.getInstance().getGson().toJsonTree(this).getAsJsonObject();
        }
    }
}
