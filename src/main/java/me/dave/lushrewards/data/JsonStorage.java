package me.dave.lushrewards.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.dave.lushrewards.LushRewards;
import org.enchantedskies.EnchantedStorage.Storage;

import java.io.*;
import java.util.UUID;

public class JsonStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    private final File dataFolder = new File(LushRewards.getInstance().getDataFolder(), "data");

    @Override
    public DataManager.StorageData load(DataManager.StorageLocation storageLocation) {
        UUID uuid = storageLocation.uuid();
        String module = storageLocation.moduleId();
        String path = module != null ? module : "main";

        JsonObject json = loadFile(uuid);
        JsonObject moduleJson = json.has(path) ? json.get(path).getAsJsonObject() : null;

        return new DataManager.StorageData(uuid, module, moduleJson);
    }

    @Override
    public void save(DataManager.StorageData storageData) {
        UUID uuid = storageData.uuid();
        String module = storageData.moduleId();
        JsonObject moduleJson = storageData.json();
        if (moduleJson == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        JsonObject json = loadFile(uuid);
        json.add(module != null ? module : "main", moduleJson);

        File file = new File(dataFolder, uuid.toString());

        try {
            new Gson().toJson(storageData.json(), new FileWriter(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonObject loadFile(UUID uuid) {
        File file = new File(dataFolder, uuid.toString());

        try {
            return JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
        } catch (FileNotFoundException e) {
            return new JsonObject();
        }
    }
}
