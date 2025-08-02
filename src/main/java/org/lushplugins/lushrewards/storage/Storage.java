package org.lushplugins.lushrewards.storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsUserData;
import org.lushplugins.lushrewards.user.ModuleUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.utils.Debugger;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public interface Storage {
    // TODO: Remove when possible
    JsonElement EMPTY_MAP_ELEMENT = LushRewards.GSON.toJsonTree(new HashMap<>());

    default void enable(ConfigurationSection config) {}

    default void disable() {}

    // TODO: RewardUser (not ModuleUserData) should not be stored as a json object
    // New format should columns should be: uuid, username, minutesPlayed
    // Module data should continue to be stored as json in the modules table
    default RewardUser loadRewardUser(UUID uuid) {
        JsonObject json = loadModuleUserDataJson(uuid, null);
        if (json == null) {
            Debugger.sendDebugMessage("No storage data found for '%s' for reward user, creating default data!"
                .formatted(uuid), Debugger.DebugMode.ALL);

            return new RewardUser(uuid, null);
        }

        try {
            json.addProperty("uuid", uuid.toString());
            json.add("moduleData", EMPTY_MAP_ELEMENT);

            return LushRewards.GSON.fromJson(json, RewardUser.class);
        } catch (Throwable e) {
            LushRewards.getInstance().log(Level.WARNING, "Caught error when parsing user data:", e);
            return null;
        }
    }

    default void saveRewardUser(RewardUser user) {
        UUID uuid = user.getUniqueId();
        JsonObject json = user.asJson();
        if (json == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        saveModuleUserDataJson(uuid, null, json);
    }

    default <T extends ModuleUserData> T loadModuleUserData(UUID uuid, @NotNull String moduleId, Class<T> userDataType) {
        JsonObject json = loadModuleUserDataJson(uuid, moduleId);
        if (json == null) {
            Debugger.sendDebugMessage("No storage data found for '%s' for module '%s', creating default data!"
                .formatted(uuid, moduleId), Debugger.DebugMode.ALL);

            try {
                return userDataType.getConstructor(UUID.class, String.class).newInstance(uuid, moduleId);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                LushRewards.getInstance().getLogger().warning("No default data found for '%s'".formatted(moduleId));
                return null;
            }
        }

        try {
            json.addProperty("uuid", uuid.toString());
            json.addProperty("moduleId", moduleId);

            T userData = LushRewards.GSON.fromJson(json, userDataType);
            if (userData == null) {
                return null;
            }

            if (userData instanceof PlaytimeRewardsUserData playtimeUserData) {
                PlaytimeRewardsModule module = LushRewards.getInstance().getRewardModuleManager().getModule(moduleId, PlaytimeRewardsModule.class);
                if (module != null) {
                    int resetPlaytimeAt = module.getResetPlaytimeAt();
                    if (resetPlaytimeAt > 0 && !playtimeUserData.getStartDate().isAfter(LocalDate.now().minusDays(resetPlaytimeAt))) {
                        playtimeUserData.setStartDate(LocalDate.now());
                        playtimeUserData.setPreviousDayEndPlaytime(playtimeUserData.getLastCollectedPlaytime());
                        this.saveModuleUserData(userData);
                    }
                }
            }

            return userData;
        } catch (Throwable e) {
            LushRewards.getInstance().log(Level.WARNING, "Caught error when parsing user data:", e);
            return null;
        }
    }

    JsonObject loadModuleUserDataJson(UUID uuid, String moduleId);

    void saveModuleUserDataJson(UUID uuid, String moduleId, JsonObject json);

    default void saveModuleUserData(ModuleUserData userData) {
        UUID uuid = userData.getUniqueId();
        String moduleId = userData.getModuleId();
        JsonObject json = userData.asJson();
        if (json == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        this.saveModuleUserDataJson(uuid, moduleId, json);
    }

    // TODO: Implement with new RewardUser storage structure
//    Collection<String> findSimilarUsernames(String input);
}
