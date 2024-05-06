package me.dave.lushrewards.command.subcommand;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.importer.ConfigImporter;
import me.dave.lushrewards.importer.DailyRewardsPlusImporter;
import me.dave.lushrewards.importer.NDailyRewardsImporter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO: Test
public class ImportSubCommand extends SubCommand {

    public ImportSubCommand() {
        super("import");
        addRequiredPermission("lushrewards.import");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        if (args.length == 0) {
            ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("incorrect-usage")
                .replace("%command-usage%", "/rewards import <plugin>"));
        }

        ConfigImporter configImporter = null;
        try {
            switch (args[1].toLowerCase()) {
                case "dailyrewardsplus" -> configImporter = new DailyRewardsPlusImporter();
                case "ndailyrewards" -> configImporter = new NDailyRewardsImporter();
            }
        } catch (FileNotFoundException e) {
            ChatColorHandler.sendMessage(sender, "&#ff6969Could not find files when attempting to import from &#d13636'" + args[1] + "'");
            return true;
        }

        if (configImporter != null) {
            long startMs = Instant.now().toEpochMilli();
            configImporter.startImport()
                .completeOnTimeout(false, 10, TimeUnit.SECONDS)
                .thenAccept(success -> {
                    if (success) {
                        ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully imported configuration from &#66b04f'" + args[1] + "' &#b7faa2in &#66b04f" + (Instant.now().toEpochMilli() - startMs) + "ms");
                        LushRewards.getInstance().getConfigManager().reloadConfig();
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reload"));
                    } else {
                        ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + args[1] + "' &#ff6969in &#d13636" + (Instant.now().toEpochMilli() - startMs) + "ms");
                    }
                });
        } else {
            ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + args[1] + "'");
        }

        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        return List.of("DailyRewardsPlus");
    }
}