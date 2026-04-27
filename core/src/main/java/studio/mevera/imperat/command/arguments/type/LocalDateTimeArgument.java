package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.time.LocalDateTime;

/**
 * Parses an input token into a {@link LocalDateTime} using ISO-8601
 * (e.g. {@code 2026-04-27T05:00:00}).
 */
public final class LocalDateTimeArgument<S extends CommandSource> extends SimpleArgumentType<S, LocalDateTime> {

    @Override
    public LocalDateTime parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return LocalDateTime.parse(input);
        } catch (Exception ex) {
            throw new CommandException("Invalid ISO date-time: '%s' (expected e.g. 2026-04-27T05:00:00)", input);
        }
    }
}
