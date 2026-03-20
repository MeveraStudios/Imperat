package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;

public final class CharacterArgument<S extends CommandSource> extends ArgumentType<S, Character> {
    @Override
    public Character parse(@NotNull CommandContext<S> context, @NotNull String input) throws ResponseException {
        if (input.length() > 1) {
            throw new ArgumentParseException(ResponseKey.INVALID_CHARACTER, input)
                          .withContextPlaceholders(context);
        }
        return input.charAt(0);
    }

    @Override
    public Character parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws ResponseException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new IllegalArgumentException("No input available at cursor position");
        }
        return parse(context, input);
    }
}
