package me.dave.lushrewards.module;

import me.dave.lushrewards.utils.Keyed;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface UserDataModule<T extends UserDataModule.UserData> {

    Class<T> getUserDataClass();

    T getDefaultData(UUID uuid);

    T getUserData(UUID uuid);

    void cacheUserData(UUID uuid, UserData userData);

    void uncacheUserData(UUID uuid);

    String getStorageProviderName();

    abstract class UserData implements Keyed {
        private final UUID uuid;
        protected final String id;

        public UserData(UUID uuid, String id) {
            this.uuid = uuid;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public @NotNull String getKey() {
            return uuid.toString();
        }
    }
}
