package org.enchantedskies.activityrewarder.datamanager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.UUID;

public class DataManager {
    private final Storage storage;
    private final HashMap<UUID, RewardUser> uuidToRewardUser = new HashMap<>();

    public DataManager() {
        storage = new YmlStorage();
    }

    public void loadRewardUser(UUID uuid) {
        uuidToRewardUser.put(uuid, storage.loadRewardUser(uuid));
    }

    public void saveRewardUser(RewardUser rewardUser) {
        storage.saveRewardUser(rewardUser);
    }

    public RewardUser getRewardUser(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return null;
        return uuidToRewardUser.getOrDefault(uuid, new RewardUser(uuid, player.getName(), LocalDate.now().toString(), LocalDate.now().minusDays(1).toString(), 1, 0));
    }
}
