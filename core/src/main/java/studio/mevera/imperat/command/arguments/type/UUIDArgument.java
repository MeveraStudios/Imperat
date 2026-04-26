package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.UUID;

public final class UUIDArgument<S extends CommandSource> extends SimpleArgumentType<S, UUID> {

    public UUIDArgument() {
        super();
    }

    @Override
    public UUID parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException {
        try {
            return UUID.fromString(input);
        } catch (Exception ex) {
            throw new ArgumentParseException(ResponseKey.INVALID_UUID, input).withContextPlaceholders(context);
        }
    }

}
