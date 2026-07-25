package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.storage.Storage;
import org.lushplugins.lushrewards.user.RewardUser;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class JsonStorage implements Storage {
    private final File storageDir = new File(LushRewards.getInstance().getDataFolder(), "data");

    @Override
    public @Nullable RewardUser prepareRewardUser(UUID uuid) {
        JsonObject json = loadFile(uuid);
        if (!json.has("main")) {
            return null;
        }

        JsonObject userJson = json.getAsJsonObject("main");
        return new RewardUser(
            uuid,
            userJson.has("username") ? userJson.get("username").getAsString() : null,
            userJson.has("minutesPlayed") ? userJson.get("minutesPlayed").getAsInt() : 0
        );
    }

    @Override
    public void saveRewardUser(RewardUser user) {
        assertStorageDir();

        UUID uuid = user.getUniqueId();
        JsonObject json = loadFile(uuid);
        json.add("main", LushRewards.GSON.toJsonTree(user).getAsJsonObject());

        try {
            FileWriter writer = new FileWriter(getUserFile(uuid));
            LushRewards.GSON.toJson(json, writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonObject loadModuleUserDataJson(UUID uuid, @NotNull String moduleId) {
        JsonObject json = loadFile(uuid);
        return json.has(moduleId) ? json.get(moduleId).getAsJsonObject() : null;
    }

    @Override
    public void saveModuleUserDataJson(UUID uuid, String moduleId, JsonObject moduleJson) {
        assertStorageDir();

        if (moduleJson == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        JsonObject json = loadFile(uuid);
        json.add(moduleId != null ? moduleId : "main", moduleJson);
        try {
            FileWriter writer = new FileWriter(getUserFile(uuid));
            LushRewards.GSON.toJson(json, writer);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> findSimilarUsernames(String input) {
        return Collections.emptyList();
    }

    private void assertStorageDir() {
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
    }

    private JsonObject loadFile(UUID uuid) {
        assertStorageDir();

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
