package org.enchantedskies.activityrewarder;

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
    private final RewardGUI rewardGUI = new RewardGUI();
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

            ChatColorHandler.sendMessage(player, ActivityRewarder.configManager.getReloadMessage());
        } else {
            if (!player.hasPermission("activityrewarder.use")) {
                player.sendMessage("§cInsufficient permissions");
                return true;
            }
            rewardGUI.openGUI(player);
            guiPlayerSet.add(player.getUniqueId());
        }
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
