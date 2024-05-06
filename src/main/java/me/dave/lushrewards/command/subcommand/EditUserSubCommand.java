package me.dave.lushrewards.command.subcommand;

import joptsimple.internal.Strings;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.module.RewardModule;
import me.dave.lushrewards.module.dailyrewards.DailyRewardsModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.module.Module;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditUserSubCommand extends SubCommand {

    public EditUserSubCommand() {
        super("edit-user");
        addRequiredArgs(0, () -> {
            List<String> modules = new ArrayList<>(LushRewards.getInstance().getEnabledRewardModules().stream().map(Module::getId).toList());
            modules.add("*");
            return modules;
        });
        addSubCommand(new ResetSubCommand());
        addSubCommand(new ResetDaysSubCommand());
        addSubCommand(new SetDaysSubCommand());
        addSubCommand(new ResetStreakSubCommand());
        addSubCommand(new SetStreakSubCommand());
        addRequiredPermission("lushrewards.edituser");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
            .replace("%command-usage%", "/rewards edit-user <module-id>"));
        return true;
    }

    private static class ResetSubCommand extends SubCommand {

        public ResetSubCommand() {
            super("reset");
            addRequiredPermission("lushrewards.edituser.reset");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                    .replace("%command-usage%", "/rewards edit-user <module-id> reset <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reset")
                    .replace("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> reset <player> confirm"));
                        return true;
                    }

                    List<RewardModule> modules = getModules(fullArgs[1]);
                    if (!setDay(sender, args[0], modules, 1) || !setStreak(sender, args[0], modules, 1) || !removeCollectedDays(sender, args[0], modules)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-days-confirm")
                        .replace("%target%", args[0])
                        .replace("%day%", "1")
                        .replace("%module%", Strings.join(modules.stream().map(Module::getId).toList(), ", ")));
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
    }

    private static class ResetDaysSubCommand extends SubCommand {

        public ResetDaysSubCommand() {
            super("reset-days");
            addRequiredPermission("lushrewards.edituser.resetdays");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                    .replace("%command-usage%", "/rewards edit-user <module-id> reset-days <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reset-days")
                    .replace("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> reset-days <player> confirm"));
                        return true;
                    }

                    List<RewardModule> modules = getModules(fullArgs[1]);
                    if (!setDay(sender, args[0], modules, 1)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-days-confirm")
                        .replace("%target%", args[0])
                        .replace("%day%", "1")
                        .replace("%module%", Strings.join(modules.stream().map(Module::getId).toList(), ", ")));
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
    }

    private static class SetDaysSubCommand extends SubCommand {

        public SetDaysSubCommand() {
            super("set-days");
            addRequiredPermission("lushrewards.edituser.setdays");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
            switch (args.length) {
                case 0, 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                    .replace("%command-usage%", "/rewards edit-user <module-id> set-days <player> <day-num>"));
                case 2 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-days")
                    .replace("%target%", args[0])
                    .replace("%day%", args[1]));
                case 3 -> {
                    if (!args[2].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> set-days <player> <day-num> confirm"));
                        return true;
                    }

                    int dayNum;
                    try {
                        dayNum = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> set-days <player> <day-num> confirm"));
                        return true;
                    }

                    List<RewardModule> modules = getModules(fullArgs[1]);
                    if (!setDay(sender, args[0], modules, dayNum)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-days-confirm")
                        .replace("%target%", args[0])
                        .replace("%day%", String.valueOf(dayNum))
                        .replace("%module%", Strings.join(modules.stream().map(Module::getId).toList(), ", ")));
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
    }

    private static class ResetStreakSubCommand extends SubCommand {

        public ResetStreakSubCommand() {
            super("reset-streak");
            addRequiredPermission("lushrewards.edituser.resetstreak");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                    .replace("%command-usage%", "/rewards edit-user <module-id> reset-streak <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reset-streak")
                    .replace("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> reset-streak <player> confirm"));
                        return true;
                    }

                    List<RewardModule> modules = getModules(fullArgs[1]);
                    if (!setStreak(sender, args[0], modules, 1)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-streak-confirm")
                        .replace("%target%", args[0])
                        .replace("%streak%", "1")
                        .replace("%module%", Strings.join(modules.stream().map(Module::getId).toList(), ", ")));
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
    }

    private static class SetStreakSubCommand extends SubCommand {

        public SetStreakSubCommand() {
            super("set-streak");
            addRequiredPermission("lushrewards.edituser.setstreak");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
            switch (args.length) {
                case 0, 1 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                    .replace("%command-usage%", "/rewards edit-user <module-id> set-streak <player> <streak>"));
                case 2 -> ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-streak")
                    .replace("%target%", args[0])
                    .replace("%streak%", args[1]));
                case 3 -> {
                    if (!args[2].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> set-streak <player> <streak> confirm"));
                        return true;
                    }

                    int streak;
                    try {
                        streak = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                            .replace("%command-usage%", "/rewards edit-user <module-id> set-streak <player> <streak> confirm"));
                        return true;
                    }

                    List<RewardModule> modules = getModules(fullArgs[1]);
                    if (!setStreak(sender, args[0], modules, streak)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("set-streak-confirm")
                        .replace("%target%", args[0])
                        .replace("%streak%", String.valueOf(streak))
                        .replace("%module%", Strings.join(modules.stream().map(Module::getId).toList(), ", ")));
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
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean setDay(CommandSender sender, String nameOrUuid, List<RewardModule> modules, int dayNum) {
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

        modules.forEach(module -> {
            if (module instanceof DailyRewardsModule dailyRewardsModule) {
                dailyRewardsModule.getOrLoadUserData(uuid, false).thenAccept(userData -> {
                    if (userData != null) {
                        userData.setDayNum(dayNum);
                        userData.setLastCollectedDate(LocalDate.of(1971, 10, 1)); // The date Walt Disney World was opened
                        dailyRewardsModule.saveUserData(uuid, userData);
                    }
                });
            }
        });

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean setStreak(CommandSender sender, String nameOrUuid, List<RewardModule> modules, int streak) {
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

        modules.forEach(module -> {
            if (module instanceof DailyRewardsModule dailyRewardsModule) {
                dailyRewardsModule.getOrLoadUserData(uuid, false).thenAccept(userData -> {
                    userData.setStreak(streak);
                    dailyRewardsModule.saveUserData(uuid, userData);
                });
            }
        });

        return true;
    }

    private static boolean removeCollectedDays(CommandSender sender, String nameOrUuid, List<RewardModule> modules) {
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

        if (player != null) {
            modules.forEach(module -> {
                if (module instanceof DailyRewardsModule dailyRewardsModule) {
                    dailyRewardsModule.getOrLoadUserData(uuid, false).thenAccept(userData -> {
                        userData.clearCollectedDays();
                        userData.setLastCollectedDate(null);

                        dailyRewardsModule.saveUserData(uuid, userData);
                    });
                }
            });
        }

        return true;
    }

    private static List<RewardModule> getModules(String moduleNames) {
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

    private List<String> getRemainingModules(String arg) {
        List<String> possibleModules = new ArrayList<>(LushRewards.getInstance().getEnabledRewardModules().stream()
            .map(Module::getId)
            .toList()
        );

        for (String moduleName : arg.split(",")) {
            possibleModules.remove(moduleName);
        }

        return possibleModules;
    }
}
