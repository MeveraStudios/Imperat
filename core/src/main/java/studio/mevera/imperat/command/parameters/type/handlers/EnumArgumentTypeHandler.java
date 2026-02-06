package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.EnumArgument;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

/**
 * Handler for resolving {@link ArgumentType} instances for enum types.
 * <p>
 * This handler automatically creates an {@link EnumArgument} for any type
 * that is or extends {@link Enum}.
 * </p>
 *
 * @param <S> the source type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class EnumArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return TypeUtility.areRelatedTypes(type, Enum.class);
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        return (ArgumentType<S, T>) new EnumArgument<>((TypeWrap<Enum<?>>) wrap);
    }

    @Override
    public @NotNull Priority priority() {
        // Low priority so specific enum types registered by user take precedence
        return Priority.LOW;
    }
}
