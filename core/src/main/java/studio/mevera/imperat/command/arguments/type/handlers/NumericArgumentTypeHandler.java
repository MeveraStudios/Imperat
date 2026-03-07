package studio.mevera.imperat.command.arguments.type.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeLookup;
import studio.mevera.imperat.command.arguments.type.ArgumentTypes;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

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
public final class NumericArgumentTypeHandler<S extends CommandSource> implements ArgumentTypeHandler<S> {

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
    public @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }
}
