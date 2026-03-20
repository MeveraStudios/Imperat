package studio.mevera.imperat.tests.commands.realworld.economy;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

import java.math.BigDecimal;

public class BigDecimalParamType extends ArgumentType<TestCommandSource, BigDecimal> {


    @Override
    public @NotNull BigDecimal parse(@NotNull CommandContext<TestCommandSource> context, @NotNull String correspondingInput) throws CommandException {
        try {
            double d = Double.parseDouble(correspondingInput);
            return BigDecimal.valueOf(d);
        } catch (Exception ex) {
            throw new CommandException("Invalid decimal: " + correspondingInput, ex);
        }
    }

}
