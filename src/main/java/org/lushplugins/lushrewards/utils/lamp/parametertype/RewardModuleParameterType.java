package org.lushplugins.lushrewards.utils.lamp.parametertype;

import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.RewardModule;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public class RewardModuleParameterType implements ParameterType<CommandActor, RewardModule> {

    @Override
    public RewardModule parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<CommandActor> context) {
        return LushRewards.getInstance().getRewardModuleManager().getModule(input.source());
    }

    @Override
    public @NotNull SuggestionProvider<CommandActor> defaultSuggestions() {
        // TODO: Add permission check to only tab-complete modules that the user has permission for
        return (context) -> LushRewards.getInstance().getRewardModuleManager().getModules().stream()
            .map(RewardModule::getId)
            .toList();
    }
}
