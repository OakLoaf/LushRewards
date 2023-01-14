package org.enchantedskies.activityrewarder.rewards;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.geysermc.floodgate.api.FloodgateApi;

public class CmdReward implements Reward {
    private final String command;


    public CmdReward(String commandReward) {
        this.command = commandReward;
    }

    @Override
    public void giveReward(Player player) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        String[] commandArr = command.split("\\|");
        boolean isFloodgateEnabled = ActivityRewarder.isFloodgateEnabled();
        for (String thisCommand : commandArr) {
            thisCommand = thisCommand.replaceAll("%user%", player.getName());
//            thisCommand = thisCommand.replaceAll("%user%", player.getName()).replaceAll("%hourly-amount%", String.valueOf(hourlyAmount));
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
