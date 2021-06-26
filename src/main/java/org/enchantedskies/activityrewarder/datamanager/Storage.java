package org.enchantedskies.activityrewarder.datamanager;

import java.util.UUID;

public interface Storage {
    RewardUser loadRewardUser(UUID uuid);
    void saveRewardUser(RewardUser followerUser);
}
