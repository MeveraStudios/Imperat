package studio.mevera.imperat.command.arguments.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeLookup;
import studio.mevera.imperat.command.arguments.type.EnumArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

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
@SuppressWarnings({"unchecked"})
public final class EnumArgumentTypeHandler<S extends CommandSource> implements ArgumentTypeHandler<S> {

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
    public @NotNull Priority getPriority() {
        // Low priority so specific enum types registered by user take precedence
        return Priority.LOW;
    }
}
