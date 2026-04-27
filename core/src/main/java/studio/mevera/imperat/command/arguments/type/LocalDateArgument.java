package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.time.LocalDate;

/**
 * Parses an input token into a {@link LocalDate} using ISO-8601
 * (e.g. {@code 2026-04-27}).
 */
public final class LocalDateArgument<S extends CommandSource> extends SimpleArgumentType<S, LocalDate> {

    @Override
    public LocalDate parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return LocalDate.parse(input);
        } catch (Exception ex) {
            throw new CommandException("Invalid ISO date: '%s' (expected e.g. 2026-04-27)", input);
        }
    }
}
