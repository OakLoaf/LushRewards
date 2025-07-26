package org.lushplugins.lushrewards.utils.lamp.parametertype;

import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushrewards.migrator.importer.DailyRewardsPlusImporter;
import org.lushplugins.lushrewards.migrator.importer.NDailyRewardsImporter;
import org.lushplugins.lushrewards.migrator.Migrator;
import org.lushplugins.lushrewards.migrator.Version3DataMigrator;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.io.FileNotFoundException;
import java.util.List;

public class MigratorParameterType implements ParameterType<CommandActor, Migrator> {

    @Override
    public Migrator parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<CommandActor> context) {
        String argument = input.source();

        try {
            return switch (argument.toLowerCase()) {
                case "dailyrewardsplus" -> new DailyRewardsPlusImporter();
                case "ndailyrewards" -> new NDailyRewardsImporter();
                case "version2to3" -> new Version3DataMigrator();
                default -> throw new CommandErrorException("'%s' is not a valid importer/migrator".formatted(argument));
            };
        } catch (FileNotFoundException e) {
            throw new CommandErrorException("Could not find files when attempting to import from '%s'".formatted(argument));
        }
    }

    @Override
    public @NotNull SuggestionProvider<CommandActor> defaultSuggestions() {
        return (context) -> List.of("DailyRewardsPlus", "NDailyRewards", "Version2to3");
    }
}
