package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.hooks.PlaceholderAPIHook;
import me.dave.activityrewarder.utils.SchedulerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCommandReward extends Reward {
    private final List<String> commands;

    public PlayerCommandReward(List<String> commands) {
        this.commands = commands;
    }

    @SuppressWarnings("unchecked")
    public PlayerCommandReward(Map<?, ?> map) {
        this.commands = (List<String>) map.get("commands");
    }

    @Override
    protected void giveTo(Player player) {
        boolean isFloodgateEnabled = ActivityRewarder.isFloodgateEnabled();
        commands.forEach(command -> {
            String thisCommand = command.replaceAll("%user%", player.getName()).replaceAll("%player%", player.getName());

            PlaceholderAPIHook placeholderAPIHook = ActivityRewarder.getPlaceholderAPIHook();
            if (placeholderAPIHook != null) {
                thisCommand = placeholderAPIHook.parseString(player, thisCommand);
            }

            if (thisCommand.startsWith("java:")) {
                thisCommand = thisCommand.substring(5);

                if (isFloodgateEnabled && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    return;
                }
            } else if (thisCommand.startsWith("bedrock:")) {
                thisCommand = thisCommand.substring(8);

                if (!isFloodgateEnabled) {
                    return;
                } else if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    return;
                }
            }

            Bukkit.dispatchCommand(player, thisCommand);
        });

    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new ConcurrentHashMap<>();

        rewardMap.put("type", "player-command");
        rewardMap.put("commands", commands);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.PLAYER;
    }
}
