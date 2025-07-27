package org.lushplugins.lushrewards.utils.lamp.contextparameter;

import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.command.CommandParameter;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ContextParameter;

public class RewardUserContextParameter implements ContextParameter<BukkitCommandActor, RewardUser> {

    @Override
    public RewardUser resolve(@NotNull CommandParameter parameter, @NotNull ExecutionContext<BukkitCommandActor> context) {
        return LushRewards.getInstance().getDataManager().getRewardUser(context.actor().requirePlayer());
    }
}
