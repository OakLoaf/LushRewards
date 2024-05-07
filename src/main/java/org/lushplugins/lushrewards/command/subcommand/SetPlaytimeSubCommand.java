package org.lushplugins.lushrewards.command.subcommand;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.RewardModule;
import org.lushplugins.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.module.playtimetracker.PlaytimeTrackerModule;

import java.util.List;
import java.util.UUID;

public class SetPlaytimeSubCommand extends SubCommand {

    public SetPlaytimeSubCommand() {
        super("set-playtime");
        addRequiredPermission("lushrewards.edituser.setplaytime");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        switch (args.length) {
            case 0, 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                .replace("%command-usage%", "/rewards edit-user <module-id> set-playtime <player> <playtime>"));
            case 2 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-playtime")
                .replace("%target%", args[0])
                .replace("%playtime%", args[1]));
            case 3 -> {
                if (!args[2].equalsIgnoreCase("confirm")) {
                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                        .replace("%command-usage%", "/rewards edit-user <module-id> set-playtime <player> <playtime> confirm"));
                    return true;
                }

                int playtime;
                try {
                    playtime = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                        .replace("%command-usage%", "/rewards edit-user <module-id> set-playtime <player> <playtime> confirm"));
                    return true;
                }

                if (!setPlaytime(sender, args[0], playtime)) {
                    return true;
                }

                ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-playtime-confirm")
                    .replace("%target%", args[0])
                    .replace("%playtime%", String.valueOf(playtime)));
                return true;
            }
        }

        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        } else {
            return null;
        }
    }

    private boolean setPlaytime(CommandSender sender, String nameOrUuid, int playtime) {
        Player player = Bukkit.getPlayer(nameOrUuid);
        UUID uuid;
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            try {
                uuid = UUID.fromString(nameOrUuid);
            } catch (IllegalArgumentException e) {
                ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("unknown-player")
                    .replace("%player%", nameOrUuid));
                return false;
            }
        }

        RewardUser rewardUser = LushRewards.getInstance().getDataManager().getRewardUser(uuid);
        if (rewardUser != null) {
            LushRewards.getInstance().getModule(RewardModule.Type.PLAYTIME_TRACKER).ifPresent(module -> ((PlaytimeTrackerModule) module).getPlaytimeTracker(uuid).setGlobalPlaytime(playtime));
            rewardUser.setMinutesPlayed(playtime);

            LushRewards.getInstance().getRewardModules().forEach(module -> {
                if (module instanceof PlaytimeRewardsModule playtimeRewardsModule) {
                    PlaytimeRewardsModule.UserData userData = playtimeRewardsModule.getUserData(uuid);
                    if (userData.getLastCollectedPlaytime() > playtime) {
                        userData.setLastCollectedPlaytime(playtime);
                    }
                }
            });

            return true;
        } else {
            return false;
        }
    }
}