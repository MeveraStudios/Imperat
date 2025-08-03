package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

public final class JavaDurationParameterType extends BaseParameterType<TestSource, Duration> {

    public JavaDurationParameterType() {
        super();
    }

    @Override
    public @Nullable Duration resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> inputStream,
            @NotNull String input
    ) throws ImperatException {
        return JavaDurationParser.parseDuration(input);
    }

    /**
     * Determines whether the provided input matches the expected format or criteria
     * for a given command parameter. this is used during
     *
     * @param input     The input strings to be matched against the parameter criteria.
     * @param parameter The command parameter that provides context for the input handling.
     * @return true if the input matches the expected criteria; false otherwise.
     */
    @Override
    public boolean matchesInput(String input, CommandParameter<TestSource> parameter) {
        try {
            JavaDurationParser.parseDuration(input);
            return true;
        }catch (IllegalArgumentException ex) {
            return false;
        }
    }
}