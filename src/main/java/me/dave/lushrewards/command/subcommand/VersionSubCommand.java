package me.dave.lushrewards.command.subcommand;

import me.dave.lushrewards.LushRewards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

public class VersionSubCommand extends SubCommand {

    public VersionSubCommand() {
        super("version");
        addRequiredPermission("lushrewards.version");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        ChatColorHandler.sendMessage(sender, "&#a8e1ffYou are currently running &#58b1e0LushRewards &#a8e1ffversion &#58b1e0" + LushRewards.getInstance().getDescription().getVersion());
        return true;
    }
}