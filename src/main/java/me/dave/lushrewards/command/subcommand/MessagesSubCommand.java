package me.dave.lushrewards.command.subcommand;

import me.dave.lushrewards.LushRewards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

public class MessagesSubCommand extends SubCommand {

    public MessagesSubCommand() {
        super("messages");
        addRequiredPermission("lushrewards.viewmessages");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        LushRewards.getInstance().getConfigManager().getMessages().forEach(message -> ChatColorHandler.sendMessage(sender, message));
        return true;
    }
}