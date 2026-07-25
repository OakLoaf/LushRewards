package org.lushplugins.lushrewards.utils.lamp.parametertype;

import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.command.CommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

import java.time.LocalDate;

public class LocalDateParameterType implements ParameterType<CommandActor, LocalDate> {

    @Override
    public LocalDate parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<CommandActor> context) {
        return LocalDate.parse(input.readString());
    }

    @Override
    public @NotNull SuggestionProvider<CommandActor> defaultSuggestions() {
        return SuggestionProvider.of("yyyy-mm-dd");
    }
}
