package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Either;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

/**
 * Handler for resolving {@link ArgumentType} instances for {@link Either} types.
 *
 * @param <S> the source type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class EitherArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.getRawType().equals(Either.class);
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var parameterizedTypes = wrap.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length < 2) {
            throw new IllegalArgumentException("Either requires two type parameters");
        }

        TypeWrap<?> typeA = TypeWrap.of(parameterizedTypes[0]);
        TypeWrap<?> typeB = TypeWrap.of(parameterizedTypes[1]);

        return (ArgumentType<S, T>) ArgumentTypes.either(
                (TypeWrap<Either<Object, Object>>) wrap,
                (TypeWrap<Object>) typeA,
                (TypeWrap<Object>) typeB
        );
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
