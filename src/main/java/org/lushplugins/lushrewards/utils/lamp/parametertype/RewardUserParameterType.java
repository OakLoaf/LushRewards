package org.lushplugins.lushrewards.utils.lamp.parametertype;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.user.RewardUser;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

// TODO: Add support for offline users(?)
public class RewardUserParameterType implements ParameterType<BukkitCommandActor, RewardUser> {

    @Override
    public RewardUser parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<BukkitCommandActor> context) {
        Player player = Bukkit.getPlayer(input.readString());
        if (player == null) {
            throw new CommandErrorException("Could not find reward user data for user '%s'".formatted(input.source()));
        }

        return LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
    }

    @Override
    public @NotNull SuggestionProvider<BukkitCommandActor> defaultSuggestions() {
        return (context) -> LushRewards.getInstance().getUserCache().getCachedUsers().stream()
            .map(RewardUser::getUsername)
            .toList();
    }
}
