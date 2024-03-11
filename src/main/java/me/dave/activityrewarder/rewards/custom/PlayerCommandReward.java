package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.hooks.FloodgateHook;
import me.dave.activityrewarder.hooks.HookId;
import me.dave.activityrewarder.utils.SchedulerType;
import me.dave.platyutils.hook.Hook;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.libraries.chatcolor.parsers.custom.PlaceholderAPIParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
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
        commands.forEach(commandRaw -> {
            commandRaw = commandRaw.replaceAll("%user%", player.getName()).replaceAll("%player%", player.getName());
            String command = ChatColorHandler.translate(commandRaw, player, List.of(PlaceholderAPIParser.class));

            boolean dispatchCommand = true;
            if (command.startsWith("java:")) {
                command = command.substring(5);

                Optional<Hook> optionalHook = ActivityRewarder.getInstance().getHook(HookId.FLOODGATE.toString());
                if (optionalHook.isPresent() && ((FloodgateHook) optionalHook.get()).isFloodgatePlayer(player.getUniqueId())) {
                    dispatchCommand = false;
                }
            } else if (command.startsWith("bedrock:")) {
                command = command.substring(8);

                Optional<Hook> optionalHook = ActivityRewarder.getInstance().getHook(HookId.FLOODGATE.toString());
                if (optionalHook.isEmpty() || !((FloodgateHook) optionalHook.get()).isFloodgatePlayer(player.getUniqueId())) {
                    dispatchCommand = false;
                }
            }

            if (dispatchCommand) {
                Bukkit.dispatchCommand(player, command);
            }
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
