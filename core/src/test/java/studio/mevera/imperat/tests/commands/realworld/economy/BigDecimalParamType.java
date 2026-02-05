package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

import java.math.BigDecimal;

public class BigDecimalParamType extends ArgumentType<TestSource, BigDecimal> {


    @Override
    public @Nullable BigDecimal parse(@NotNull ExecutionContext<TestSource> context, @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        try {
            double d = Double.parseDouble(correspondingInput);
            return BigDecimal.valueOf(d);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, Argument<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            double d = Double.parseDouble(input);
            BigDecimal.valueOf(d);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
