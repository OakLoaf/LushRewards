package me.dave.activityrewarder.commands;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.custom.RewardsGui;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class RewardCmd implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1 && args[0].equals("reload")) {
            if (!sender.hasPermission("activityrewarder.reload")) {
                ChatColorHandler.sendMessage(sender, "&cInsufficient permissions");
                return true;
            }
            ActivityRewarder.configManager.reloadConfig();

            ChatColorHandler.sendMessage(sender, ActivityRewarder.configManager.getReloadMessage());
            return true;
        }

        if (!(sender instanceof Player player)) {
            ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
            return true;
        }

        if (!player.hasPermission("activityrewarder.use")) {
            player.sendMessage("§cInsufficient permissions");
            return true;
        }

        RewardsGui rewardsGui = new RewardsGui(player);
        rewardsGui.openInventory();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {
        List<String> tabComplete = new ArrayList<>();
        List<String> wordCompletion = new ArrayList<>();
        boolean wordCompletionSuccess = false;
        if (!commandSender.hasPermission("activityrewarder.use")) return tabComplete;
        if (args.length == 1) {
            if (commandSender.hasPermission("activityrewarder.reload")) tabComplete.add("reload");
        }

        for (String currTab : tabComplete) {
            int currArg = args.length - 1;
            if (currTab.startsWith(args[currArg])) {
                wordCompletion.add(currTab);
                wordCompletionSuccess = true;
            }
        }
        if (wordCompletionSuccess) return wordCompletion;
        return tabComplete;
    }
}
