package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.MapArgument;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

/**
 * Handler for resolving {@link ArgumentType} instances for {@link Map} types.
 * <p>
 * This handler supports all standard Java map implementations and allows
 * registration of custom map initializers.
 * </p>
 *
 * @param <S> the source type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class MapArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    private final Map<Type, Supplier<Map<?, ?>>> initializers = new LinkedHashMap<>();

    /**
     * Creates a new MapArgumentTypeHandler with default initializers.
     */
    public MapArgumentTypeHandler() {
        // Standard Map implementations
        initializers.put(HashMap.class, HashMap::new);
        initializers.put(LinkedHashMap.class, LinkedHashMap::new);
        initializers.put(TreeMap.class, TreeMap::new);
        initializers.put(WeakHashMap.class, WeakHashMap::new);
        initializers.put(IdentityHashMap.class, IdentityHashMap::new);

        // Concurrent Map implementations
        initializers.put(ConcurrentHashMap.class, ConcurrentHashMap::new);
        initializers.put(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new);

        // EnumMap requires special handling
        initializers.put(EnumMap.class, () -> {
            throw new UnsupportedOperationException("EnumMap requires an enum type parameter");
        });

        // Sorted/Navigable Map interfaces
        initializers.put(SortedMap.class, TreeMap::new);
        initializers.put(NavigableMap.class, TreeMap::new);

        // Interface defaults
        initializers.put(Map.class, HashMap::new);
    }

    /**
     * Registers a custom map initializer.
     *
     * @param type     the map type
     * @param supplier the supplier that creates new instances
     * @param <M>      the map type
     */
    public <M extends Map<?, ?>> void registerInitializer(@NotNull Class<M> type, @NotNull Supplier<M> supplier) {
        initializers.put(type, (Supplier<Map<?, ?>>) supplier);
    }

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.isSubtypeOf(Map.class);
    }

    @Override
    public <T> @NotNull ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var parameterizedTypes = wrap.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length < 2) {
            throw new IllegalArgumentException("Raw map types are not allowed");
        }

        TypeWrap<?> keyType = TypeWrap.of(parameterizedTypes[0]);
        TypeWrap<?> valueType = TypeWrap.of(parameterizedTypes[1]);

        ArgumentType<S, ?> keyResolver = lookup.lookupOrThrow(keyType.getType());
        ArgumentType<S, ?> valueResolver = lookup.lookupOrThrow(valueType.getType());

        Supplier<Map<?, ?>> initializer = findInitializer(wrap);

        @SuppressWarnings("unchecked")
        Supplier<Map<Object, Object>> typedInitializer = (Supplier<Map<Object, Object>>) (Supplier<?>) initializer;

        return (ArgumentType<S, T>) new MapArgument<>(
                (TypeWrap<Map<Object, Object>>) wrap,
                typedInitializer,
                (ArgumentType<S, Object>) keyResolver,
                (ArgumentType<S, Object>) valueResolver
        );
    }

    private Supplier<Map<?, ?>> findInitializer(TypeWrap<?> wrap) {
        Type rawType = wrap.getRawType();

        // Direct match
        Supplier<Map<?, ?>> direct = initializers.get(rawType);
        if (direct != null) {
            return direct;
        }

        // Search for compatible supertype
        for (var entry : initializers.entrySet()) {
            if (TypeWrap.of(rawType).isSupertypeOf(entry.getKey())) {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Unknown map type: " + rawType.getTypeName());
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
