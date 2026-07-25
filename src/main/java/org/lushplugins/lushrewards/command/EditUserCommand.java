package org.lushplugins.lushrewards.command;

import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsUserData;
import org.lushplugins.lushrewards.user.RewardUser;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@SuppressWarnings("unused")
@Command("rewards user")
public class EditUserCommand {

    @Subcommand("get <user> playtime")
    @CommandPermission("lushrewards.edituser.dailyrewards.daynum.get")
    public String getDayNum(BukkitCommandActor actor, RewardUser user) {
        return "&#66b04f%s's &#b7faa2playtime is &#66b04f%s"
            .formatted(user.getUsername(), user.getMinutesPlayed());
    }

    @Subcommand("modify <user> playtime")
    @CommandPermission("lushrewards.user.playtime.set")
    public void setPlaytime(BukkitCommandActor actor, RewardUser user, int playtime) {
        for (PlaytimeRewardsUserData userData : user.getAllCachedModuleData(PlaytimeRewardsUserData.class)) {
            if (userData.getLastCollectedPlaytime() > playtime) {
                userData.setLastCollectedPlaytime(playtime);
            }

            if (userData.getPreviousDayEndPlaytime() > playtime) {
                userData.setPreviousDayEndPlaytime(playtime);
            }
        }

        // The below method also saves the user, so we rely on this
        user.setMinutesPlayed(playtime);
        ChatColorHandler.sendMessage(actor.sender(), "&#66b04f%s's &#b7faa2playtime has been set to &#66b04f%s"
            .formatted(user.getUsername(), playtime));
    }

    @Subcommand("reset <user> playtime")
    @CommandPermission("lushrewards.user.playtime.reset")
    public void resetPlaytime(BukkitCommandActor actor, RewardUser user) {
        this.setPlaytime(actor, user, 0);
    }
}
