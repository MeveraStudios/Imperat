package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Parses an input token as a {@link Pattern}, compiled at parse time so
 * malformed regexes fail fast with a useful error message.
 */
public final class PatternArgument<S extends CommandSource> extends SimpleArgumentType<S, Pattern> {

    @Override
    public Pattern parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return Pattern.compile(input);
        } catch (PatternSyntaxException ex) {
            throw new CommandException("Invalid regex: '%s' (%s)", input, ex.getDescription());
        }
    }
}
