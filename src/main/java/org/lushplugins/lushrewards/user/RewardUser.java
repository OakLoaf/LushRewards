package org.lushplugins.lushrewards.user;

import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.api.event.RewardUserPlaytimeChangeEvent;
import org.lushplugins.rewardsapi.api.RewardsAPI;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private int minutesPlayed;
    private final Map<String, ModuleUserData> moduleData;

    public RewardUser(UUID uuid, @Nullable String username, int minutesPlayed, Map<String, ModuleUserData> moduleData) {
        this.uuid = uuid;
        this.username = username;
        this.minutesPlayed = minutesPlayed;
        this.moduleData = moduleData;
    }

    public RewardUser(UUID uuid, @Nullable String username, int minutesPlayed) {
        this(uuid, username, minutesPlayed, new HashMap<>());
    }

    public RewardUser(UUID uuid, @Nullable String username) {
        this(uuid, username, 0, new HashMap<>());
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;

        LushRewards.getInstance().getStorageManager().saveRewardUser(this);
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public void setMinutesPlayed(int minutesPlayed, boolean save) {
        RewardsAPI.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> LushRewards.getInstance().callEvent(new RewardUserPlaytimeChangeEvent(this, this.minutesPlayed, minutesPlayed)));

        this.minutesPlayed = minutesPlayed;

        if (save) {
            LushRewards.getInstance().getStorageManager().saveRewardUser(this);
        }
    }

    public void setMinutesPlayed(int minutesPlayed) {
        setMinutesPlayed(minutesPlayed, true);
    }

    public ModuleUserData getCachedModuleData(String moduleId) {
        return moduleData.get(moduleId);
    }

    public <T extends ModuleUserData> T getCachedModuleData(String moduleId, Class<T> userDataType) {
        ModuleUserData userData = moduleData.get(moduleId);
        return userDataType.isInstance(userData) ? userDataType.cast(userData) : null;
    }

    public <T extends ModuleUserData> CompletableFuture<T> getModuleData(String moduleId, Class<T> userDataType) {
        return LushRewards.getInstance().getStorageManager().loadModuleUserData(uuid, moduleId, userDataType).thenApply(userData -> {
            moduleData.put(moduleId, userData);
            return userData;
        });
    }

    public void cacheModuleData(ModuleUserData userData) {
        moduleData.put(userData.getModuleId(), userData);
    }

    public Collection<ModuleUserData> getAllCachedModuleData() {
        return moduleData.values();
    }

    public <T extends ModuleUserData> Collection<T> getAllCachedModuleData(Class<T> moduleType) {
        return moduleData.values().stream()
            .filter(moduleType::isInstance)
            .map(moduleType::cast)
            .toList();
    }
}
