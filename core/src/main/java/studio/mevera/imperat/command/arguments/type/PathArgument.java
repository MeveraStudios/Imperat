package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Parses an input token into a filesystem {@link Path}. Does not validate
 * that the path exists — callers can layer that check via a validator.
 */
public final class PathArgument<S extends CommandSource> extends SimpleArgumentType<S, Path> {

    @Override
    public Path parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return Paths.get(input);
        } catch (InvalidPathException ex) {
            throw new CommandException("Invalid path: '%s' (%s)", input, ex.getReason());
        }
    }
}
