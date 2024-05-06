package me.dave.lushrewards.command;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.command.subcommand.*;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsGui;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.Command;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.module.Module;

import java.util.ArrayList;
import java.util.List;

public class RewardsCommand extends Command {

    public RewardsCommand() {
        super("rewards");
        addSubCommand(new AboutSubCommand());
        addSubCommand(new ClaimSubCommand());
        addSubCommand(new GuiSubCommand());
        addSubCommand(new EditUserSubCommand());
        addSubCommand(new ImportSubCommand());
        addSubCommand(new MessagesSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new UpdateSubCommand());
        addSubCommand(new VersionSubCommand());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        LushRewards.getInstance().getConfigManager().checkRefresh();

        if (!(sender instanceof Player player)) {
            ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
            return true;
        }

        if (!player.hasPermission("lushrewards.use")) {
            ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("no-permissions"));
            return true;
        }

        LushRewards.getInstance().getModules().stream()
            .filter(module -> module instanceof DailyRewardsModule)
            .findFirst()
            .ifPresentOrElse(
                module -> new DailyRewardsGui((DailyRewardsModule) module, player).open(),
                () -> ChatColorHandler.sendMessage(player, "&#ff6969Daily Rewards module is disabled")
            );

        return true;
    }
}
