package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.gui.GuiDisplayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.module.Module;

import java.util.List;

// TODO: Migrate to Lamp
public class GuiSubCommand extends SubCommand {

    public GuiSubCommand() {
        super("gui");
        addRequiredPermission("lushrewards.use");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be ran by a player");
            return true;
        }

        if (args.length < 1) {
            ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage", "&#ff6969Incorrect usage try &#d13636%command-usage%")
                    .replace("%command-usage%", "/rewards gui <module>"));
        } else {
            String moduleId = args[0];

            if (!player.hasPermission("lushrewards.use." + moduleId)) {
                ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("no-permissions", "&#ff6969Insufficient permissions"));
                return true;
            }

            LushRewards.getInstance().getModule(moduleId).ifPresent(module -> {
                if (module instanceof GuiDisplayer guiDisplayer) {
                    guiDisplayer.displayGui(player);
                }
            });
        }

        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        return LushRewards.getInstance().getEnabledRewardModules().stream().map(Module::getId).toList();
    }
}
