package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Either;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.util.TypeWrap;

public class EitherArgument<S extends CommandSource, A, B> extends ArgumentType<S, Either<A, B>> {
    private final TypeWrap<A> primaryType;
    private final TypeWrap<B> fallbackType;

    EitherArgument(
            TypeWrap<Either<A, B>> type,
            TypeWrap<A> primaryType,
            TypeWrap<B> fallbackType
    ) {
        super(type.getType());
        this.primaryType = primaryType;
        this.fallbackType = fallbackType;
    }

    @Override
    public Either<A, B> parse(@NotNull CommandContext<S> context, @NotNull String input) {
        throw new UnsupportedOperationException("EitherArgument does not support parse(context, String)");
    }

    @Override
    public Either<A, B> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException, ResponseException {
        var cfg = context.imperatConfig();
        ArgumentType<S, A> primaryArgType = (ArgumentType<S, A>) cfg.getArgumentType(primaryType.getType());
        ArgumentType<S, B> fallbackArgType = (ArgumentType<S, B>) cfg.getArgumentType(fallbackType.getType());
        if (primaryArgType == null || fallbackArgType == null) {
            throw new CommandException("Neither primary nor fallback argument type is registered for '%s' or '%s'"
                    .formatted(primaryType.getType().getTypeName(), fallbackType.getType().getTypeName()));
        }
        Cursor<S> cursorCopy = cursor.copy();
        try {
            A primaryValue = primaryArgType.parse(context, cursorCopy);
            if (primaryValue != null) {
                cursor.setAt(cursorCopy);
                return Either.ofPrimary(primaryValue);
            }
        } catch (Exception ignored) {
            // Try fallback
        }
        B fallbackValue = fallbackArgType.parse(context, cursor);
        return Either.ofFallback(fallbackValue);
    }
}
