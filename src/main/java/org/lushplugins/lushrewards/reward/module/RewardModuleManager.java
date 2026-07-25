package org.lushplugins.lushrewards.reward.module;

import org.bukkit.configuration.file.YamlConfiguration;
import org.lushplugins.lushlib.utils.FilenameUtils;
import org.lushplugins.lushrewards.LushRewards;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

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

        try {
            Files.newDirectoryStream(modulesDirectory.toPath(), "*.yml").forEach(entry -> {
                File moduleFile = entry.toFile();
                YamlConfiguration moduleConfig = YamlConfiguration.loadConfiguration(moduleFile);
                if (!moduleConfig.getBoolean("enabled", true)) {
                    return;
                }

                String moduleId = FilenameUtils.removeExtension(moduleFile.getName());
                String rewardModuleType;
                if (moduleConfig.contains("type")) {
                    rewardModuleType = moduleConfig.getString("type");
                } else if (moduleId.contains("playtime")) {
                    rewardModuleType = "playtime-rewards";
                } else {
                    rewardModuleType = moduleId;
                }

                if (rewardModuleType != null && RewardModuleTypes.contains(rewardModuleType)) {
                    modules.put(moduleId, RewardModuleTypes.constructModuleType(rewardModuleType, moduleId, moduleConfig));
                } else {
                    LushRewards.getInstance().getLogger().severe("Module with id '%s' failed to register due to invalid value at 'type'"
                        .formatted(moduleId));
                }
            });
        } catch (IOException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Something went wrong whilst reading modules files", e);
        }

        if (this.modules.isEmpty()) {
            throw new IllegalStateException("Failed to find any rewards modules in the modules directory");
        }

        if (this.modules.values().stream().anyMatch(RewardModule::requiresPlaytimeTracker)) {
            LushRewards.getInstance().getPlaytimeTrackerManager().enable();
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
