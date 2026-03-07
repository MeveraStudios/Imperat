package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.UUID;

public final class UUIDArgument<S extends CommandSource> extends ArgumentType<S, UUID> {

    public UUIDArgument() {
        super();
    }

    @Override
    public @NotNull UUID parse(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws CommandException {

        try {
            return UUID.fromString(correspondingInput);
        } catch (Exception ex) {
            throw new ArgumentParseException(ResponseKey.INVALID_UUID, correspondingInput);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<S> context, Argument<S> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            UUID.fromString(input);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
