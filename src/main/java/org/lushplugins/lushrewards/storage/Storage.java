package org.lushplugins.lushrewards.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.user.ModuleUserData;
import org.lushplugins.lushrewards.user.RewardUser;

import java.util.Collection;
import java.util.UUID;

public interface Storage {

    default void enable(ConfigurationSection config) {}

    default void disable() {}

    RewardUser loadRewardUser(UUID uuid);

    void saveRewardUser(RewardUser user);

    void saveModuleUserData(ModuleUserData userData);

    Collection<String> findSimilarUsernames(String input);
}
