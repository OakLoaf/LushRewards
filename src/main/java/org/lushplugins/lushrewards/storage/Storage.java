package org.lushplugins.lushrewards.storage;

import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import org.lushplugins.lushrewards.reward.module.StoresUserData;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsUserData;
import org.lushplugins.lushrewards.user.ModuleUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.utils.Debugger;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;

// Columns in user table should be: uuid, username, minutesPlayed
// Module data should continue to be stored as json in the modules table
public interface Storage {

    default void enable(ConfigurationSection config) {}

    default void disable() {}

    @Nullable RewardUser prepareRewardUser(UUID uuid);

    default RewardUser loadRewardUser(UUID uuid) {
        RewardUser user = prepareRewardUser(uuid);
        if (user == null) {
            Debugger.sendDebugMessage("No storage data found for '%s' for reward user, creating default data!"
                .formatted(uuid), Debugger.DebugMode.ALL);

            return new RewardUser(uuid, null);
        }

        try {
            for (RewardModule module : LushRewards.getInstance().getRewardModuleManager().getModules()) {
                StoresUserData userDataAnnotation = module.getClass().getAnnotation(StoresUserData.class);
                if (userDataAnnotation != null) {
                    ModuleUserData userData = loadModuleUserData(uuid, module.getId(), userDataAnnotation.value());
                    user.cacheModuleData(userData);
                }
            }

            return user;
        } catch (Throwable e) {
            LushRewards.getInstance().log(Level.WARNING, "Caught error when parsing user data:", e);
            return null;
        }
    }

    void saveRewardUser(RewardUser user);

    JsonObject loadModuleUserDataJson(UUID uuid, @NotNull String moduleId);

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

            // TODO: Migrate somewhere in PlaytimeRewardsUserData (or module)?
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

    /**
     * Save a RewardUser and all cached module user data
     */
    default void saveEntireRewardUser(RewardUser user) {
        saveRewardUser(user);
        user.getAllCachedModuleData().forEach(this::saveModuleUserData);
    }

    Collection<String> findSimilarUsernames(String input);
}
