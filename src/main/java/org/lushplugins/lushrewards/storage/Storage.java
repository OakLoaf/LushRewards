package org.lushplugins.lushrewards.storage;

import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.module.UserDataModule;

import java.util.UUID;

public abstract class Storage {

    public void enable(ConfigurationSection config) {}

    public void disable() {}

    public abstract JsonObject loadModuleUserDataJson(UUID uuid, String moduleId);

    public abstract void saveModuleUserData(UserDataModule.UserData userData);
}
