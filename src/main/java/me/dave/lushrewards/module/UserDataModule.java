package me.dave.lushrewards.module;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface UserDataModule<T extends UserDataModule.UserData> {

    T getDefaultData(UUID uuid);

    T getUserData(UUID uuid);

    void cacheUserData(UUID uuid, UserData userData);

    void uncacheUserData(UUID uuid);

    Class<T> getUserDataClass();

    abstract class UserData {
        private final UUID uuid;
        private final String moduleId;

        public UserData(UUID uuid, String moduleId) {
            this.uuid = uuid;
            this.moduleId = moduleId;
        }

        public @NotNull String getUniqueId() {
            return uuid.toString();
        }

        public String getModuleId() {
            return moduleId;
        }

        public abstract JsonElement asJson();
    }
}
