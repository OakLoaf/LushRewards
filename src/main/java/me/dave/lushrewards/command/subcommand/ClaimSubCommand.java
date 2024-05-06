package me.dave.lushrewards.command.subcommand;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.UserDataModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class ClaimSubCommand extends SubCommand {

    public ClaimSubCommand() {
        super("claim");
        addRequiredPermission("lushrewards.use");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        if (!(sender instanceof Player player)) {
            ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
            return true;
        }

        AtomicInteger rewardsGiven = new AtomicInteger();
        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
            if (module instanceof UserDataModule<?> userDataModule) {
                userDataModule.getOrLoadUserData(player.getUniqueId(), true).thenAccept(userData -> module.claimRewards(player));
                rewardsGiven.getAndIncrement();
            } else {
                module.claimRewards(player);
                rewardsGiven.getAndIncrement();
            }
        });

        if (rewardsGiven.get() == 0) {
            ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("no-rewards-available"));
        }

        return true;
    }
}
