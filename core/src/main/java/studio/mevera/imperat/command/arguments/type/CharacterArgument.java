package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.responses.ResponseKey;

public final class CharacterArgument<S extends CommandSource> extends SimpleArgumentType<S, Character> {
    @Override
    public Character parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws ResponseException {
        if (input.length() > 1) {
            throw new ArgumentParseException(ResponseKey.INVALID_CHARACTER, input)
                          .withContextPlaceholders(context);
        }
        return input.charAt(0);
    }

}
