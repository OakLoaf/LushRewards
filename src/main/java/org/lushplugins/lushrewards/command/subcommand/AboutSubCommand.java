package org.lushplugins.lushrewards.command.subcommand;

import org.lushplugins.lushrewards.LushRewards;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.command.SubCommand;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;

public class AboutSubCommand extends SubCommand {
    private static final String[] ABOUT_MESSAGES = new String[]{
        "&#A5B8FE&lLushRewards &#C4B6FE(v" + LushRewards.getInstance().getDescription().getVersion() + ")",
        "&7An extremely configurable, feature rich rewards plugin. ",
        "&7Reward your players each day for logging in and also reward ",
        "&7them for their time spent on the server with playtime rewards!",
        "&r",
        "&7Author: &#f7ba6fLushPlugins",
        "&r",
        "&7Wiki: &#f7ba6fhttps://docs.lushplugins.org/lush-rewards",
        "&7Support: &#f7ba6fhttps://discord.gg/p3duRZsZ2f"
    };

    public AboutSubCommand() {
        super("about");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args, @NotNull String[] fullArgs) {
        for (String aboutMessage : ABOUT_MESSAGES) {
            ChatColorHandler.sendMessage(sender, aboutMessage);
        }
        return true;
    }
}
