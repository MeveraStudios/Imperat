package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
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
    
    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, CommandParameter<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            System.out.println("MATCHING DURATION");
            JavaDurationParser.parseDuration(input);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}