package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

/**
 * Handler for resolving {@link ArgumentType} instances for numeric types.
 * <p>
 * This handler supports all standard Java numeric types including:
 * {@code byte, short, int, long, float, double} and their wrapper classes.
 * </p>
 *
 * @param <S> the source type
 */
@SuppressWarnings({"unchecked"})
public final class NumericArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return TypeUtility.isNumericType(wrap);
    }

    @Override
    public @Nullable <T> ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        Type boxedType = TypeUtility.primitiveToBoxed(type);
        if (!(boxedType instanceof Class<?> clazz)) {
            return null;
        }
        return (ArgumentType<S, T>) ArgumentTypes.numeric((Class<? extends Number>) clazz);
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
