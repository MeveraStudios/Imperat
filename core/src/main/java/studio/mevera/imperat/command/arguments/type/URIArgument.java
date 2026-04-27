package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Parses an input token into a {@link URI}. Use when accepting webhook
 * endpoints, RPC targets, or any external resource identifier.
 */
public final class URIArgument<S extends CommandSource> extends SimpleArgumentType<S, URI> {

    @Override
    public URI parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        try {
            return new URI(input);
        } catch (URISyntaxException ex) {
            throw new CommandException("Invalid URI: '%s' (%s)", input, ex.getReason());
        }
    }
}
