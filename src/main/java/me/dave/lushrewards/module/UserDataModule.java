package me.dave.lushrewards.module;

import java.util.UUID;

public interface UserDataModule<T extends UserDataModule.UserData> {

    Class<T> getUserDataClass();

    T getDefaultData();

    T getUserData(UUID uuid);

    void loadUserData(UUID uuid, UserData userData);

    void unloadUserData(UUID uuid);

    abstract class UserData {
        protected final String id;

        public UserData(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @FunctionalInterface
        public interface Constructor {
            UserData build();
        }
    }
}
