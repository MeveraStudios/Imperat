package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.math.BigDecimal;

/**
 * Parses an input token into an arbitrary-precision {@link BigDecimal}.
 * Useful for currency / financial commands where {@code double} rounding
 * is unacceptable.
 */
public final class BigDecimalArgument<S extends CommandSource> extends SimpleArgumentType<S, BigDecimal> {

    @Override
    public BigDecimal parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return new BigDecimal(input);
        } catch (NumberFormatException ex) {
            throw new CommandException("Invalid decimal number: '%s'", input);
        }
    }
}
