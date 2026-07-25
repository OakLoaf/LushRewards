package org.lushplugins.lushrewards.utils.lamp.parametertype;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public class RewardModuleParameterType implements ParameterType<BukkitCommandActor, RewardModule> {

    @Override
    public RewardModule parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        return LushRewards.getInstance().getRewardModuleManager().getModule(input.readString());
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {

        return (context) -> {
            Player player = context.actor().asPlayer();
            return LushRewards.getInstance().getRewardModuleManager().getModules().stream()
                .map(RewardModule::getId)
                .filter(id -> player == null || player.hasPermission("lushrewards.use.%s".formatted(id)))
                .toList();
        };
    }
}
