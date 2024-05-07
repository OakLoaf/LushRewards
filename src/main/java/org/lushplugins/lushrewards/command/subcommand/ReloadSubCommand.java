package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

public class ReloadSubCommand extends SubCommand {

    public ReloadSubCommand() {
        super("reload");
        addRequiredPermission("lushrewards.reload");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        LushRewards.getInstance().getConfigManager().reloadConfig();
        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reload"));
        return true;
    }
}
