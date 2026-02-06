package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.function.Supplier;

/**
 * An {@link ArgumentTypeHandler} that wraps a simple {@link Supplier} for non-generic types.
 * <p>
 * This handler is used for basic types like {@code String}, {@code Boolean}, {@code UUID}, etc.,
 * where the resolution doesn't require any recursive type lookups.
 * </p>
 *
 * @param <S> the source type
 * @param <T> the argument type
 */
public final class SimpleTypeResolver<S extends Source, T> implements ArgumentTypeHandler<S> {

    private final Type targetType;
    private final Supplier<ArgumentType<S, T>> supplier;
    private final Priority priority;
    private final boolean matchRelated;

    /**
     * Creates a new SimpleTypeResolver with {@link Priority#HIGH}.
     *
     * @param targetType the type this resolver handles
     * @param supplier   the supplier that creates ArgumentType instances
     */
    public SimpleTypeResolver(@NotNull Type targetType, @NotNull Supplier<ArgumentType<S, T>> supplier) {
        this(targetType, supplier, Priority.HIGH, false);
    }

    /**
     * Creates a new SimpleTypeResolver with the specified priority.
     *
     * @param targetType the type this resolver handles
     * @param supplier   the supplier that creates ArgumentType instances
     * @param priority   the priority of this resolver
     */
    public SimpleTypeResolver(@NotNull Type targetType, @NotNull Supplier<ArgumentType<S, T>> supplier, @NotNull Priority priority) {
        this(targetType, supplier, priority, false);
    }

    /**
     * Creates a new SimpleTypeResolver with full configuration.
     *
     * @param targetType   the type this resolver handles
     * @param supplier     the supplier that creates ArgumentType instances
     * @param priority     the priority of this resolver
     * @param matchRelated if true, also matches subtypes of the target type
     */
    public SimpleTypeResolver(
            @NotNull Type targetType,
            @NotNull Supplier<ArgumentType<S, T>> supplier,
            @NotNull Priority priority,
            boolean matchRelated
    ) {
        this.targetType = TypeUtility.primitiveToBoxed(targetType);
        this.supplier = supplier;
        this.priority = priority;
        this.matchRelated = matchRelated;
    }

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        Type boxed = TypeUtility.primitiveToBoxed(type);
        if (TypeUtility.matches(boxed, targetType)) {
            return true;
        }
        return matchRelated && TypeUtility.areRelatedTypes(boxed, targetType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <R> ArgumentType<S, R> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        return (ArgumentType<S, R>) supplier.get();
    }

    @Override
    public @NotNull Priority priority() {
        return priority;
    }

    /**
     * Returns the target type this resolver handles.
     *
     * @return the target type
     */
    public Type getTargetType() {
        return targetType;
    }
}
