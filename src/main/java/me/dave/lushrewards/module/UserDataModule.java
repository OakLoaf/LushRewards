package me.dave.lushrewards.module;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface UserDataModule<T extends UserDataModule.UserData> {

    T getUserData(UUID uuid);

    void cacheUserData(UUID uuid, UserData userData);

    void uncacheUserData(UUID uuid);

    T getDefaultData(UUID uuid);

    Class<T> getUserDataClass();

    abstract class UserData {
        @Expose(serialize = false, deserialize = false)
        private final UUID uuid;
        @Expose(serialize = false, deserialize = false)
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
            return new Gson().toJsonTree(this).getAsJsonObject();
        }
    }
}
