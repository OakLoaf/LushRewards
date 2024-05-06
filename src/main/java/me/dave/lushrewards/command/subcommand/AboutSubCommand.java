package me.dave.lushrewards.command.subcommand;

import me.dave.lushrewards.LushRewards;
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
        "&7Author: &#f7ba6fDav_e_",
        "&r",
        "&7Wiki: &#f7ba6fhttps://dave-12.gitbook.io/lush-rewards",
        "&7Support: &#f7ba6fhttps://discord.gg/p3duRZsZ2f"
    };

    public AboutSubCommand() {
        super("about");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        for (String aboutMessage : ABOUT_MESSAGES) {
            ChatColorHandler.sendMessage(sender, aboutMessage);
        }
        return true;
    }
}
