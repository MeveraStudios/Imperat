package dev.velix.imperat.advanced;

import dev.velix.imperat.ImperatConfig;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.type.BaseParameterType;
import dev.velix.imperat.command.tree.CommandTree;
import dev.velix.imperat.components.TestSource;
import dev.velix.imperat.context.ArgumentQueue;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public final class DurationParameterType extends BaseParameterType<TestSource, Duration> {

    public DurationParameterType() {
        super();
    }

    @Override
    public @Nullable Duration resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        return DurationParser.parseDuration(input);
    }

    /**
     * Determines whether the provided input matches the expected format or criteria
     * for a given command parameter. this is used during {@link CommandTree#contextMatch(ArgumentQueue, ImperatConfig)}
     *
     * @param input     The input strings to be matched against the parameter criteria.
     * @param parameter The command parameter that provides context for the input handling.
     * @return true if the input matches the expected criteria; false otherwise.
     */
    @Override
    public boolean matchesInput(String input, CommandParameter<TestSource> parameter) {
        try {
            DurationParser.parseDuration(input);
            return true;
        }catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
