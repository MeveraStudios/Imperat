package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

import java.math.BigDecimal;

public class BigDecimalParamType extends ArgumentType<TestCommandSource, BigDecimal> {


    @Override
    public @Nullable BigDecimal parse(@NotNull ExecutionContext<TestCommandSource> context, @NotNull Cursor<TestCommandSource> cursor,
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
    public boolean matchesInput(int rawPosition, CommandContext<TestCommandSource> context, Argument<TestCommandSource> parameter) {
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
