package me.dave.activityrewarder.module.playtimeglobalgoals;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.Module;
import me.dave.activityrewarder.rewards.collections.PlaytimeRewardCollection;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.Debugger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaytimeGlobalGoalsModule extends Module {
    private final HashMap<String, PlaytimeRewardCollection> permissionToPlaytimeReward = new HashMap<>();

    public PlaytimeGlobalGoalsModule(String id, @NotNull ConfigurationSection configurationSection) {
        super(id);

        configurationSection.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection permissionSection) {
                List<Map<?, ?>> rewardMaps = permissionSection.getMapList("rewards");
                List<Reward> rewardList = !rewardMaps.isEmpty() ? Reward.loadRewards(rewardMaps, permissionSection.getCurrentPath() + "rewards") : new ArrayList<>();

                if (rewardList != null) {
                    permissionToPlaytimeReward.put(key, new PlaytimeRewardCollection(permissionSection.getDouble("multiplier", 1), rewardList));
                }
            }
        });
    }

    @Nullable
    public PlaytimeRewardCollection getHourlyRewards(Player player) {
        Debugger.sendDebugMessage("Getting playtime rewards section from config", Debugger.DebugMode.PLAYTIME);
        if (permissionToPlaytimeReward.isEmpty()) {
            Debugger.sendDebugMessage("No playtime rewards found", Debugger.DebugMode.PLAYTIME);
            return null;
        }

        Debugger.sendDebugMessage("Checking player's highest multiplier", Debugger.DebugMode.PLAYTIME);
        PlaytimeRewardCollection playtimeRewardCollection = getHighestMultiplierReward(player);
        if (playtimeRewardCollection != null) {
            Debugger.sendDebugMessage("Found highest multiplier (" + playtimeRewardCollection.getMultiplier() + ")", Debugger.DebugMode.PLAYTIME);
            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            rewardUser.setHourlyMultiplier(playtimeRewardCollection.getMultiplier());
        } else {
            Debugger.sendDebugMessage("Could not find a valid multiplier for this player", Debugger.DebugMode.PLAYTIME);
        }

        return playtimeRewardCollection;
    }

    @Nullable
    public PlaytimeRewardCollection getHighestMultiplierReward(Player player) {
        return getHighestMultiplierResult(player).rewardCollection();
    }

    public double getHighestMultiplier(Player player) {
        return getHighestMultiplierResult(player).multiplier();
    }

    @NotNull
    private HighestMultiplierResult getHighestMultiplierResult(Player player) {
        PlaytimeRewardCollection highestMultiplierReward = null;
        double highestMultiplier = 0;

        for (Map.Entry<String, PlaytimeRewardCollection> entry : permissionToPlaytimeReward.entrySet()) {
            String permission = entry.getKey();

            if (!player.hasPermission("activityrewarder.bonus." + permission)) {
                continue;
            }
            Debugger.sendDebugMessage("Player has activityrewarder.bonus." + permission, Debugger.DebugMode.PLAYTIME);

            double multiplier = entry.getValue().getMultiplier();
            if (multiplier > highestMultiplier) {
                Debugger.sendDebugMessage("Found higher multiplier, updated highest multiplier", Debugger.DebugMode.PLAYTIME);
                highestMultiplier = multiplier;
                highestMultiplierReward = entry.getValue();
            }
        }
        return new HighestMultiplierResult(highestMultiplierReward, highestMultiplier);
    }

    private record HighestMultiplierResult(PlaytimeRewardCollection rewardCollection, double multiplier) {}
}
