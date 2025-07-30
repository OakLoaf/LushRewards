package org.lushplugins.lushrewards.reward.module;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.playtimetracker.PlaytimeTrackerModule;

import java.io.File;
import java.util.*;

public class RewardModuleManager {
    private Map<String, RewardModule> modules;

    public RewardModuleManager() {
        LushRewards plugin = LushRewards.getInstance();
        if (!new File(plugin.getDataFolder(), "modules").exists()) {
            plugin.saveDefaultResource("modules/daily-rewards.yml");
            plugin.saveDefaultResource("modules/daily-playtime-rewards.yml");
            plugin.saveDefaultResource("modules/global-playtime-rewards.yml");
        }
    }

    public void reloadModules() {
        this.modules = new HashMap<>();

        File modulesDirectory = new File(LushRewards.getInstance().getDataFolder(), "modules");



        if (this.modules.isEmpty()) {
            throw new IllegalStateException("Failed to find any rewards modules in the modules directory");
        }

        if (this.modules.values().stream().anyMatch(RewardModule::requiresPlaytimeTracker)) {
            if (LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).isEmpty()) {
                PlaytimeTrackerModule playtimeTrackerModule = new PlaytimeTrackerModule();
                LushRewards.getInstance().registerModule(playtimeTrackerModule);
                playtimeTrackerModule.enable();
            }
        } else {
            LushRewards.getInstance().unregisterModule(RewardModule.Type.PLAYTIME_TRACKER);
        }
    }

    public RewardModule getModule(String id) {
        return modules.get(id);
    }

    public <T extends RewardModule> T getModule(String id, Class<T> moduleType) {
        RewardModule module = getModule(id);
        return moduleType.isInstance(module) ? moduleType.cast(module) : null;
    }

    public Collection<RewardModule> getModules() {
        return modules.values();
    }

    public <T extends RewardModule> List<T> getModules(Class<T> moduleType) {
        return modules.values().stream()
            .filter(moduleType::isInstance)
            .map(moduleType::cast)
            .toList();
    }

    public Set<String> getModuleIds() {
        return modules.keySet();
    }
}
