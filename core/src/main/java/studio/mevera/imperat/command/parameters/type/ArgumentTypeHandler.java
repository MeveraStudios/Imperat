package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;

/**
 * Handler for resolving {@link ArgumentType} instances for complex or specialized types.
 * <p>
 * Implementations of this interface form a chain of responsibility for type resolution.
 * Each handler declares what types it can handle via {@link #canHandle(Type, TypeWrap)},
 * and when applicable, resolves an {@link ArgumentType} via {@link #resolve(Type, TypeWrap, ArgumentTypeLookup)}.
 * </p>
 * <p>
 * Handlers are ordered by {@link Priority} - handlers with higher priority are checked first.
 * This allows for specific handlers to override more general ones.
 * </p>
 *
 * @param <S> the source type
 */
public interface ArgumentTypeHandler<S extends Source> {

    /**
     * Determines if this handler can resolve the given type.
     *
     * @param type the raw type to check
     * @param wrap the wrapped type for convenience, providing additional type introspection
     * @return true if this handler can handle the type
     */
    boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap);

    /**
     * Resolves an {@link ArgumentType} for the given type.
     * <p>
     * This method is called only if {@link #canHandle(Type, TypeWrap)} returns true.
     * The provided {@link ArgumentTypeLookup} can be used to recursively resolve component types
     * for generic types (e.g., the element type of a collection).
     * </p>
     *
     * @param type   the type to resolve
     * @param wrap   the wrapped type for convenience
     * @param lookup the lookup function for resolving nested/component types
     * @param <T>    the resolved type
     * @return the resolved ArgumentType, or null if resolution fails despite canHandle returning true
     */
    @Nullable <T> ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    );


    default <T> void onRegisteringResolver(@NotNull SimpleTypeResolver<S, T> resolver) {
        // Default implementation does nothing, but can be overridden by handlers that need to register additional handlers

    }

    /**
     * Returns the priority of this handler.
     * <p>
     * Handlers with higher priority are checked first. Use {@link Priority#HIGH} for
     * specific type handlers and {@link Priority#LOW} for fallback handlers.
     * </p>
     *
     * @return the priority of this handler
     */
    @NotNull Priority priority();
}
