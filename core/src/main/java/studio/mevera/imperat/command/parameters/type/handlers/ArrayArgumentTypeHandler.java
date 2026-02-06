package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.ArrayArgument;
import studio.mevera.imperat.command.parameters.type.SimpleTypeResolver;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handler for resolving {@link ArgumentType} instances for array types.
 * <p>
 * This handler supports all array types and provides optimized initializers for
 * primitive wrapper arrays. Custom array initializers can be registered for
 * specific component types.
 * </p>
 *
 * @param <S> the source type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ArrayArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    private final Map<Type, Function<Integer, Object[]>> initializers = new LinkedHashMap<>();

    /**
     * Creates a new ArrayArgumentTypeHandler with default initializers.
     */
    public ArrayArgumentTypeHandler() {
        // Primitive wrapper array initializers
        initializers.put(Boolean.class, Boolean[]::new);
        initializers.put(Byte.class, Byte[]::new);
        initializers.put(Short.class, Short[]::new);
        initializers.put(Integer.class, Integer[]::new);
        initializers.put(Long.class, Long[]::new);
        initializers.put(Float.class, Float[]::new);
        initializers.put(Double.class, Double[]::new);
        initializers.put(Character.class, Character[]::new);
        initializers.put(String.class, String[]::new);
    }

    /**
     * Registers a custom array initializer for a specific component type.
     *
     * @param componentType the component type of the array
     * @param initializer   the function that creates new arrays of the specified size
     * @param <T>           the component type
     */
    public <T> void registerInitializer(@NotNull Class<T> componentType, @NotNull Function<Integer, T[]> initializer) {
        @SuppressWarnings("unchecked")
        Function<Integer, Object[]> typedInitializer = (Function<Integer, Object[]>) (Function<?, ?>) initializer;
        initializers.put(componentType, typedInitializer);
    }

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.isArray();
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var componentType = wrap.getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("Cannot determine array component type");
        }

        ArgumentType<S, Object> componentResolver = lookup.lookupOrThrow(componentType.getType());
        Function<Integer, Object[]> initializer = findInitializer(componentType);

        return (ArgumentType<S, T>) new ArrayArgument<>(
                (TypeWrap<Object[]>) wrap,
                initializer,
                componentResolver
        ) {};
    }

    @Override
    public <T> void onRegisteringResolver(@NotNull SimpleTypeResolver<S, T> resolver) {
        Class<T> rawType = (Class<T>) TypeWrap.of(resolver.getTargetType()).getRawType();
        registerInitializer(rawType, (length) -> (T[]) Array.newInstance(rawType, length));
    }

    private Function<Integer, Object[]> findInitializer(TypeWrap<?> componentType) {
        Type rawType = componentType.getRawType();

        // Direct match
        Function<Integer, Object[]> direct = initializers.get(rawType);
        if (direct != null) {
            return direct;
        }

        // Search for compatible type
        for (var entry : initializers.entrySet()) {
            if (componentType.isSupertypeOf(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Fallback: use reflection-based array creation
        if (rawType instanceof Class<?> clazz) {
            return size -> (Object[]) Array.newInstance(clazz, size);
        }

        throw new IllegalArgumentException("Unknown array component type: " + rawType.getTypeName());
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
