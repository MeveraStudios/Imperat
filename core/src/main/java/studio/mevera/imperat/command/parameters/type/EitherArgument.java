package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Either;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

public class EitherArgument<S extends Source, A, B> extends ArgumentType<S, Either<A, B>> {

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

    private static <A, B> Type[] loadTypeArray(Class<A> a, Class<B> b) {
        return new Type[]{a, b};
    }

    @Override
    public @Nullable Either<A, B> parse(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        var cfg = context.imperatConfig();
        ArgumentType<S, A> primaryArgType = (ArgumentType<S, A>) cfg.getArgumentType(primaryType.getType());
        ArgumentType<S, B> fallbackArgType = (ArgumentType<S, B>) cfg.getArgumentType(fallbackType.getType());

        if(primaryArgType == null || fallbackArgType == null) {
            throw new CommandException("Neither primary nor fallback argument type is registered for '%s' or '%s'"
                    .formatted(primaryType.getType().getTypeName(), fallbackType.getType().getTypeName()));
        }

        Cursor<S> cursorCopy = cursor.copy();
        A primaryValue;
        primaryValue = primaryArgType.parse(context, cursorCopy, correspondingInput);
        if(primaryValue != null) {
            cursor.setAt(cursorCopy);
            return Either.ofPrimary(primaryValue);
        }

        B fallbackValue = fallbackArgType.parse(context, cursor, correspondingInput);

        return Either.ofFallback(fallbackValue);
    }
}
