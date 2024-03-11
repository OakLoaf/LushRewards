package me.dave.lushrewards.rewards.custom;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.hooks.FloodgateHook;
import me.dave.lushrewards.hooks.HookId;
import me.dave.lushrewards.utils.SchedulerType;
import me.dave.platyutils.hook.Hook;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.libraries.chatcolor.parsers.custom.PlaceholderAPIParser;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("unused")
public class ConsoleCommandReward extends Reward {
    private static final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    private final List<String> commands;

    public ConsoleCommandReward(List<String> commands) {
        this.commands = commands;
    }

    @SuppressWarnings("unchecked")
    public ConsoleCommandReward(Map<?, ?> map) {
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

                Optional<Hook> optionalHook = LushRewards.getInstance().getHook(HookId.FLOODGATE.toString());
                if (optionalHook.isPresent() && ((FloodgateHook) optionalHook.get()).isFloodgatePlayer(player.getUniqueId())) {
                    dispatchCommand = false;
                }
            } else if (command.startsWith("bedrock:")) {
                command = command.substring(8);

                Optional<Hook> optionalHook = LushRewards.getInstance().getHook(HookId.FLOODGATE.toString());
                if (optionalHook.isEmpty() || !((FloodgateHook) optionalHook.get()).isFloodgatePlayer(player.getUniqueId())) {
                    dispatchCommand = false;
                }
            }

            if (dispatchCommand) {
                Bukkit.dispatchCommand(console, command);
            }
        });
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> rewardMap = new HashMap<>();

        rewardMap.put("type", "command");
        rewardMap.put("commands", commands);

        return rewardMap;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.GLOBAL;
    }
}
