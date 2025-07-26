package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.module.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: Migrate to Lamp
public class ClaimSubCommand extends SubCommand {

    public ClaimSubCommand() {
        super("claim");
        addRequiredPermission("lushrewards.use");
        addRequiredArgs(0, () -> {
            List<String> modules = new ArrayList<>(LushRewards.getInstance().getEnabledRewardModules().stream().map(Module::getId).toList());
            modules.add("*");
            return modules;
        });
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        if (!(sender instanceof Player player)) {
            ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
            return true;
        }

        List<RewardModule> modules = args.length >= 1 ? getModules(args[0]) : getModules("*");
        AtomicInteger rewardsGiven = new AtomicInteger();
        for (RewardModule module : modules) {
            if (!player.hasPermission("lushrewards.use." + module.getId())) {
                continue;
            }

            if (module.hasClaimableRewards(player)) {
                if (module instanceof UserDataModule<?> userDataModule) {
                    userDataModule.getOrLoadUserData(player.getUniqueId(), true).thenAccept(userData -> module.claimRewards(player));
                    rewardsGiven.getAndIncrement();
                } else {
                    module.claimRewards(player);
                    rewardsGiven.getAndIncrement();
                }
            }
        }

        if (rewardsGiven.get() == 0) {
            ChatColorHandler.sendMessage(player, LushRewards.getInstance().getConfigManager().getMessage("no-rewards-available"));
        }

        return true;
    }

    private List<RewardModule> getModules(String moduleNames) {
        List<RewardModule> modules = new ArrayList<>();

        if (moduleNames.equals("*")) {
            return LushRewards.getInstance().getEnabledRewardModules();
        }

        for (String name : moduleNames.split(",")) {
            LushRewards.getInstance().getModule(name).ifPresent(module -> {
                if (module instanceof RewardModule rewardModule) {
                    modules.add(rewardModule);
                }
            });
        }

        return modules;
    }
}
