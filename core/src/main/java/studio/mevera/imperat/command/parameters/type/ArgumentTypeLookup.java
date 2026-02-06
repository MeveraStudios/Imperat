package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Functional interface for resolving nested/component types during argument type resolution.
 * <p>
 * This interface is passed to {@link ArgumentTypeHandler}s to allow them to recursively
 * resolve {@link ArgumentType} instances for nested generic type parameters.
 * </p>
 *
 * @param <S> the source type
 */
@FunctionalInterface
public interface ArgumentTypeLookup<S extends Source> {

    /**
     * Looks up an {@link ArgumentType} for the specified type.
     *
     * @param type the type to look up
     * @param <T>  the resolved type
     * @return an optional containing the ArgumentType if found, empty otherwise
     */
    @NotNull <T> Optional<ArgumentType<S, T>> lookup(@NotNull Type type);

    /**
     * Looks up an {@link ArgumentType} for the specified type, throwing an exception if not found.
     *
     * @param type the type to look up
     * @param <T>  the resolved type
     * @return the resolved ArgumentType
     * @throws IllegalArgumentException if the type cannot be resolved
     */
    default @NotNull <T> ArgumentType<S, T> lookupOrThrow(@NotNull Type type) {
        return this.<T>lookup(type)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown type: " + type.getTypeName()));
    }
}
