package org.enchantedskies.activityrewarder.rewardtypes;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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
        String tempCommand = command;
        tempCommand = tempCommand.replaceAll("%user%", player.getName()).replaceAll("%hourly-amount%", String.valueOf(hourlyInt));
        Bukkit.dispatchCommand(console, tempCommand);
    }
}
