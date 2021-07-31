package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.enchantedskies.activityrewarder.ActivityRewarder;
import org.geysermc.floodgate.api.FloodgateApi;

public class CmdReward implements Reward {
    private final String command;
    private final String size;
    private final double hourlyAmount;

    public CmdReward(String commandReward, String size, double count, double hours) {
        this.command = commandReward;
        this.size = size;
        this.hourlyAmount = count * hours;
    }

    public CmdReward(String commandReward, String size) {
        this.command = commandReward;
        this.size = size;
        this.hourlyAmount = 0;
    }

    @Override
    public String getSize() {
        return size;
    }

    @Override
    public void giveReward(Player player) {
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        int hourlyInt = (int) Math.round(hourlyAmount);
        String[] commandArr = command.split("\\|");
        boolean isFloodgateEnabled = ActivityRewarder.isFloodgateEnabled();
        for (String thisCommand : commandArr) {
            thisCommand = thisCommand.replaceAll("%user%", player.getName()).replaceAll("%hourly-amount%", String.valueOf(hourlyInt));
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
