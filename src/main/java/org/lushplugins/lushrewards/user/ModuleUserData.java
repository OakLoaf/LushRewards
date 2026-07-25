package org.lushplugins.lushrewards.user;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;

import java.util.UUID;

public abstract class ModuleUserData {
    private final UUID uuid;
    private final String moduleId;

    public ModuleUserData(UUID uuid, String moduleId) {
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
        return LushRewards.GSON.toJsonTree(this).getAsJsonObject();
    }
}
