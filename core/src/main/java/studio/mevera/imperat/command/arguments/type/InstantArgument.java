package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.time.Instant;

/**
 * Parses an input token into an {@link Instant} using ISO-8601 (e.g.
 * {@code 2026-04-27T05:00:00Z}). For zone-local times use
 * {@link LocalDateTimeArgument} instead.
 */
public final class InstantArgument<S extends CommandSource> extends SimpleArgumentType<S, Instant> {

    @Override
    public Instant parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return Instant.parse(input);
        } catch (Exception ex) {
            throw new CommandException("Invalid ISO-8601 instant: '%s' (expected e.g. 2026-04-27T05:00:00Z)", input);
        }
    }
}
