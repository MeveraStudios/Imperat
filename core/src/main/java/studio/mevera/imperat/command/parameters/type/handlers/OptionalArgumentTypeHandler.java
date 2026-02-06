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
import java.util.Optional;

/**
 * Handler for resolving {@link ArgumentType} instances for {@link Optional} types.
 *
 * @param <S> the source type
 */
@SuppressWarnings("unchecked")
public final class OptionalArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.getRawType().equals(Optional.class);
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var parameterizedTypes = wrap.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw Optional types are not allowed");
        }

        TypeWrap<?> optionalType = TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, Object> optionalTypeResolver = lookup.lookupOrThrow(optionalType.getType());

        return (ArgumentType<S, T>) ArgumentTypes.optional(
                (TypeWrap<Optional<Object>>) wrap,
                optionalTypeResolver
        );
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
