package org.lushplugins.lushrewards.user;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.api.event.RewardUserPlaytimeChangeEvent;
import org.lushplugins.rewardsapi.api.RewardsAPI;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

        LushRewards.getInstance().getDataManager().saveRewardUser(this);
    }

    public int getMinutesPlayed() {
        return minutesPlayed;
    }

    public void setMinutesPlayed(int minutesPlayed) {
        RewardsAPI.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> LushRewards.getInstance().callEvent(new RewardUserPlaytimeChangeEvent(this, this.minutesPlayed, minutesPlayed)));

        this.minutesPlayed = minutesPlayed;

        LushRewards.getInstance().getDataManager().saveRewardUser(this);
    }

    public Collection<ModuleUserData> getAllModuleUserData() {
        return moduleData.values();
    }

    public <T extends ModuleUserData> Collection<T> getAllModuleUserData(Class<T> moduleType) {
        return moduleData.values().stream()
            .filter(moduleType::isInstance)
            .map(moduleType::cast)
            .toList();
    }

    public ModuleUserData getModuleData(String moduleId) {
        return moduleData.get(moduleId);
    }

    public <T extends ModuleUserData> T getModuleData(String moduleId, Class<T> moduleType) {
        ModuleUserData userData = moduleData.get(moduleId);
        return moduleType.isInstance(userData) ? moduleType.cast(userData) : null;
    }

    // TODO: Remove all below methods when possible
    @ApiStatus.Internal
    public void addModuleData(String moduleId, ModuleUserData userData) {
        moduleData.put(moduleId, userData);
    }

    @ApiStatus.Internal
    public JsonObject asJson() {
        return LushRewards.GSON.toJsonTree(this).getAsJsonObject();
    }
}
