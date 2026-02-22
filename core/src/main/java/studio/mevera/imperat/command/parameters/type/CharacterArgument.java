package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;

public final class CharacterArgument<S extends Source> extends ArgumentType<S, Character> {

    @Override
    public @NotNull Character parse(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        if (correspondingInput.length() > 1) {
            throw new CommandException(ResponseKey.INVALID_CHARACTER)
                          .withContextPlaceholders(context)
                          .withPlaceholder("input", correspondingInput);
        }
        return correspondingInput.charAt(0);
    }
}
