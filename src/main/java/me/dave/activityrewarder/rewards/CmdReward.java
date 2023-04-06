package me.dave.activityrewarder.rewards;

import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class CmdReward implements Reward {
    private static final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    private final String command;


    public CmdReward(String commandReward) {
        this.command = commandReward;
    }

    @Override
    public void giveReward(Player player) {
        String[] commandArr = command.split("\\|");
        boolean isFloodgateEnabled = ActivityRewarder.isFloodgateEnabled();
        for (String thisCommand : commandArr) {
            thisCommand = thisCommand.replaceAll("%user%", player.getName());
            if (thisCommand.startsWith("java:")) {
                thisCommand = thisCommand.substring(5);
                if (isFloodgateEnabled && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) continue;
            } else if (thisCommand.startsWith("bedrock:")) {
                thisCommand = thisCommand.substring(8);
                if (!isFloodgateEnabled) continue;
                if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) continue;
            }
            Bukkit.dispatchCommand(console, thisCommand);
        }
    }
}
