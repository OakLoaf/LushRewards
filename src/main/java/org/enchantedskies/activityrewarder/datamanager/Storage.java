package org.enchantedskies.activityrewarder.datamanager;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface Storage {
    ExecutorService SERVICE = Executors.newSingleThreadExecutor();
    RewardUser loadRewardUser(UUID uuid);
    void saveRewardUser(RewardUser followerUser);
    boolean init();
}
