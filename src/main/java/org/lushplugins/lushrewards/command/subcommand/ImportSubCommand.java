package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.olddata.converter.Converter;
import org.lushplugins.lushrewards.importer.DailyRewardsPlusImporter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushrewards.importer.NDailyRewardsImporter;
import org.lushplugins.lushrewards.olddata.converter.Version3DataMigrator;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;

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

        String name = args[0];
        Converter configImporter;
        try {
            switch (name.toLowerCase()) {
                case "dailyrewardsplus" -> configImporter = new DailyRewardsPlusImporter();
                case "ndailyrewards" -> configImporter = new NDailyRewardsImporter();
                case "version2to3" -> configImporter = new Version3DataMigrator();
                default -> configImporter = null;
            }
        } catch (FileNotFoundException e) {
            ChatColorHandler.sendMessage(sender, "&#ff6969Could not find files when attempting to import from &#d13636'" + name + "'");
            return true;
        }

        if (configImporter != null) {
            long startMs = Instant.now().toEpochMilli();
            LushRewards.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
                try {
                    if (configImporter.convert()) {
                        ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully imported configuration from &#66b04f'" + name + "' &#b7faa2in &#66b04f" + (Instant.now().toEpochMilli() - startMs) + "ms");
                        LushRewards.getInstance().getConfigManager().reloadConfig();
                        ChatColorHandler.sendMessage(sender, LushRewards.getInstance().getConfigManager().getMessage("reload"));
                    } else {
                        ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + name + "' &#ff6969in &#d13636" + (Instant.now().toEpochMilli() - startMs) + "ms");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + name + "' &#ff6969in &#d13636" + (Instant.now().toEpochMilli() - startMs) + "ms");
                }
            });
        } else {
            ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + name + "'");
        }

        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        return List.of("DailyRewardsPlus", "NDailyRewards", "Version2to3");
    }
}