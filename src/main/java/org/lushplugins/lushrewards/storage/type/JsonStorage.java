package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.OldUserDataModule;
import org.lushplugins.lushrewards.storage.Storage;

import java.io.*;
import java.util.UUID;

public class JsonStorage implements Storage {
    private final File storageDir = new File(LushRewards.getInstance().getDataFolder(), "data");

    @Override
    public JsonObject loadModuleUserDataJson(UUID uuid, String moduleId) {
        String path = moduleId != null ? moduleId : "main";

        JsonObject json = loadFile(uuid);
        return json.has(path) ? json.get(path).getAsJsonObject() : null;
    }

    @Override
    public void saveModuleUserData(OldUserDataModule.UserData userData) {
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        UUID uuid = userData.getUniqueId();
        String moduleId = userData.getModuleId();
        JsonObject moduleJson = userData.asJson();
        if (moduleJson == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        JsonObject json = loadFile(uuid);
        json.add(moduleId != null ? moduleId : "main", moduleJson);
        try {
            FileWriter writer = new FileWriter(getUserFile(uuid));
            LushRewards.getInstance().getGson().toJson(json, writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject loadFile(UUID uuid) {
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        try {
            JsonElement json = JsonParser.parseReader(new FileReader(getUserFile(uuid)));
            return json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
        } catch (FileNotFoundException e) {
            return new JsonObject();
        }
    }

    private File getUserFile(UUID uuid) {
        return new File(storageDir, uuid + ".json");
    }
}
