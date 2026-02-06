package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * Handler for resolving {@link ArgumentType} instances for {@link CompletableFuture} types.
 *
 * @param <S> the source type
 */
@SuppressWarnings({"unchecked"})
public final class CompletableFutureArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.getRawType().equals(CompletableFuture.class);
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var parameterizedTypes = wrap.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw CompletableFuture types are not allowed");
        }

        TypeWrap<?> futureType = TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, Object> futureTypeResolver = lookup.lookupOrThrow(futureType.getType());

        return (ArgumentType<S, T>) ArgumentTypes.future(
                (TypeWrap<CompletableFuture<Object>>) wrap,
                futureTypeResolver
        );
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
