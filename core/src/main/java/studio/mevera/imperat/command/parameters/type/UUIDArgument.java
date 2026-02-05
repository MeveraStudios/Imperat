package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.InvalidUUIDException;

import java.util.UUID;

public final class UUIDArgument<S extends Source> extends ArgumentType<S, UUID> {

    public UUIDArgument() {
        super();
    }

    @Override
    public @NotNull UUID resolve(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws CommandException {

        try {
            return UUID.fromString(correspondingInput);
        } catch (Exception ex) {
            throw new InvalidUUIDException(correspondingInput);
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
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
