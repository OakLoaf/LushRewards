package me.dave.activityrewarder.commands;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.gui.custom.RewardsGui;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RewardCmd implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1) {
            switch(args[0].toLowerCase()) {
                case "reload" -> {
                    if (!sender.hasPermission("activityrewarder.reload")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }
                    ActivityRewarder.getConfigManager().reloadConfig();

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("reload"));
                    return true;
                }
                case "reset-streak" -> {
                    if (!sender.hasPermission("activityrewarder.resetstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-streak <player>"));
                    return true;
                }
                case "set-streak" -> {
                    if (!sender.hasPermission("activityrewarder.setstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak>"));
                    return true;
                }
            }
        }
        else if (args.length == 2) {
            switch(args[0].toLowerCase()) {
                case "reset-streak" -> {
                    if (!sender.hasPermission("activityrewarder.resetstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("reset-streak").replaceAll("%target%", args[1]));
                    return true;
                }
                case "set-streak" -> {
                    if (!sender.hasPermission("activityrewarder.setstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak>"));
                    return true;
                }
            }

        }
        else if (args.length == 3) {
            switch(args[0].toLowerCase()) {
                case "reset-streak" -> {
                    if (!sender.hasPermission("activityrewarder.resetstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    if (!args[2].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-streak <player> confirm"));
                        return true;
                    }

                    setStreak(sender, args[1], 1);
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("set-streak-confirm").replaceAll("%target%", args[1]).replaceAll("%new-streak%", "1"));
                    return true;
                }
                case "set-streak" -> {
                    if (!sender.hasPermission("activityrewarder.setstreak")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("set-streak").replaceAll("%target%", args[1]).replaceAll("%new-streak%", "1"));
                    return true;
                }
            }
        }
        else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("set-streak")) {
                if (!sender.hasPermission("activityrewarder.setstreak")) {
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("no-permissions"));
                    return true;
                }

                if (!args[3].equalsIgnoreCase("confirm")) {
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak> confirm"));
                    return true;
                }

                int streak;
                try {
                    streak = Integer.parseInt(args[2]);
                } catch(NumberFormatException e) {
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak> confirm"));
                    return true;
                }

                if (!setStreak(sender, args[1], streak)) return true;
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("set-streak-confirm").replaceAll("%target%", args[1]).replaceAll("%new-streak%", String.valueOf(streak)));
                return true;
            }
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
            if (commandSender.hasPermission("activityrewarder.resetstreak") || commandSender.hasPermission("activityrewarder.resetstreak.others")) tabComplete.add("reset-streak");
            if (commandSender.hasPermission("activityrewarder.setstreak")) tabComplete.add("set-streak");
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

    private void setStreak(CommandSender sender, String nameOrUuid, int streak) {
        Player player = Bukkit.getPlayer(nameOrUuid);
        UUID uuid;
        if (player != null) {
            uuid = player.getUniqueId();
        }
        else {
            try {
                uuid = UUID.fromString(nameOrUuid);
            } catch(IllegalArgumentException e) {
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak>"));
                return;
            }
        }

        if (player != null && ActivityRewarder.getDataManager().isRewardUserLoaded(uuid)) {
            RewardUser rewardUser = ActivityRewarder.getDataManager().getRewardUser(player);
            rewardUser.setDay(streak);
        }
        else {
            ActivityRewarder.getDataManager().loadRewardUser(uuid).thenAccept((rewardUser -> {
                rewardUser.setDay(streak);
                ActivityRewarder.getDataManager().unloadRewarderUser(uuid);
            }));
        }
    }
}
