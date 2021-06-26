package org.enchantedskies.activityrewarder;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.UUID;

public class RewardCmd implements CommandExecutor {
    private final HashSet<UUID> guiPlayerSet;

    public RewardCmd(HashSet<UUID> guiPlayerSet) {
        this.guiPlayerSet = guiPlayerSet;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Console cannot run this command!");
            return true;
        }
        if (args.length == 1 && args[0].equals("reload")) {
            if (!player.hasPermission("activityrewarder.reload")) {
                player.sendMessage("§cInsufficient permissions");
                return true;
            }
            ActivityRewarder.configManager.reloadConfig();
            player.sendMessage("§aConfig reloaded");
        } else {
            new RewardGUI(player);
            guiPlayerSet.add(player.getUniqueId());
        }
        return true;
    }
}
