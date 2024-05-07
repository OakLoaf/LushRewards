package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.Updater;

public class UpdateSubCommand extends SubCommand {

    public UpdateSubCommand() {
        super("update");
        addRequiredPermission("lushrewards.update");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        Updater updater = LushRewards.getInstance().getUpdater();

        if (updater.isAlreadyDownloaded() || !updater.isUpdateAvailable()) {
            ChatColorHandler.sendMessage(sender, "&#ff6969It looks like there is no new update available!");
            return true;
        }

        updater.downloadUpdate().thenAccept(success -> {
            if (success) {
                ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully updated LushRewards, restart the server to apply changes!");
            } else {
                ChatColorHandler.sendMessage(sender, "&#ff6969Failed to update LushRewards!");
            }
        });

        return true;
    }
}
