package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.UUID;

public final class UUIDArgument<S extends CommandSource> extends ArgumentType<S, UUID> {

    public UUIDArgument() {
        super();
    }

    @Override
    public UUID parse(@NotNull CommandContext<S> context, @NotNull String input) throws ResponseException {
        try {
            return UUID.fromString(input);
        } catch (Exception ex) {
            throw new ArgumentParseException(ResponseKey.INVALID_UUID, input).withContextPlaceholders(context);
        }
    }

    @Override
    public UUID parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws ResponseException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return parse(context, input);
    }
}
