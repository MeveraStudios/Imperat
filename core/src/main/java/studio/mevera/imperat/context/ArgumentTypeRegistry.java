package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.command.parameters.type.SimpleTypeResolver;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.command.parameters.type.handlers.ArrayArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.CollectionArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.CompletableFutureArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.EitherArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.EnumArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.MapArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.NumericArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.handlers.OptionalArgumentTypeHandler;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.PriorityList;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registry for {@link ArgumentType} instances, managing type resolution through a chain of handlers.
 * <p>
 * This registry uses a {@link PriorityList} of {@link ArgumentTypeHandler}s to resolve types.
 * Handlers are checked in priority order (highest first), and the first handler that can
 * handle a type is used to resolve it.
 * </p>
 * <p>
 * Simple types can be registered directly using {@link #registerResolver(Type, Supplier)},
 * which internally creates a {@link SimpleTypeResolver}. Complex types like collections,
 * arrays, and maps are handled by specialized handlers that can be customized.
 * </p>
 *
 * @param <S> the source type
 */
@ApiStatus.Internal
public final class ArgumentTypeRegistry<S extends Source> {

    private final PriorityList<ArgumentTypeHandler<S>> handlers = new PriorityList<>();

    private final ArrayArgumentTypeHandler<S> arrayHandler = new ArrayArgumentTypeHandler<>();
    private final CollectionArgumentTypeHandler<S> collectionHandler = new CollectionArgumentTypeHandler<>();
    private final MapArgumentTypeHandler<S> mapHandler = new MapArgumentTypeHandler<>();

    private ArgumentTypeRegistry() {
        // Register simple type resolvers (highest priority for exact matches)
        registerResolver(Boolean.class, ArgumentTypes::bool, Priority.HIGH);
        registerResolver(String.class, ArgumentTypes::string, Priority.HIGH);
        registerResolver(UUID.class, ArgumentTypes::uuid, Priority.HIGH);

        // Register built-in handlers for complex types
        registerHandler(arrayHandler);
        registerHandler(collectionHandler);
        registerHandler(mapHandler);
        registerHandler(new CompletableFutureArgumentTypeHandler<>());
        registerHandler(new OptionalArgumentTypeHandler<>());
        registerHandler(new EitherArgumentTypeHandler<>());
        registerHandler(new NumericArgumentTypeHandler<>());
        registerHandler(new EnumArgumentTypeHandler<>());
    }

    /**
     * Creates a new default ArgumentTypeRegistry with all built-in handlers registered.
     *
     * @param <S> the source type
     * @return a new ArgumentTypeRegistry instance
     */
    public static <S extends Source> ArgumentTypeRegistry<S> createDefault() {
        return new ArgumentTypeRegistry<>();
    }

    private void publishResolver(@NotNull SimpleTypeResolver<S, ?> resolver) {
        for (ArgumentTypeHandler<S> handler : handlers) {
            handler.onRegisteringResolver(resolver);
        }
    }
    /**
     * Registers a custom {@link ArgumentTypeHandler}.
     * <p>
     * The handler will be added to the priority list and checked during type resolution
     * based on its priority.
     * </p>
     *
     * @param handler the handler to register
     */
    public void registerHandler(@NotNull ArgumentTypeHandler<S> handler) {
        handlers.add(handler.priority(), handler);
    }

    /**
     * Registers a simple type resolver for a specific type.
     * <p>
     * This is a convenience method that creates a {@link SimpleTypeResolver} internally.
     * Use this for non-generic types like {@code String}, {@code UUID}, etc.
     * </p>
     *
     * @param type     the type to register
     * @param supplier the supplier that creates ArgumentType instances
     * @param <T>      the resolved type
     */
    public <T> void registerResolver(@NotNull Type type, @NotNull Supplier<ArgumentType<S, T>> supplier) {
        registerResolver(type, supplier, Priority.HIGH);

    }

    /**
     * Registers a simple type resolver for a specific type with a custom priority.
     *
     * @param type     the type to register
     * @param supplier the supplier that creates ArgumentType instances
     * @param priority the priority for this resolver
     * @param <T>      the resolved type
     */
    public <T> void registerResolver(@NotNull Type type, @NotNull Supplier<ArgumentType<S, T>> supplier, @NotNull Priority priority) {
        SimpleTypeResolver<S, T> resolver = new SimpleTypeResolver<>(type, supplier, priority);
        handlers.add(priority, resolver);
        publishResolver(resolver);
    }

    /**
     * Registers a custom collection initializer.
     * <p>
     * This is a convenience method that delegates to {@link CollectionArgumentTypeHandler}.
     * </p>
     *
     * @param type                the collection type
     * @param initializerFunction the supplier that creates new collection instances
     * @param <C>                 the collection type
     */
    public <C extends Collection<?>> void registerCollectionInitializer(
            @NotNull Class<C> type,
            @NotNull Supplier<C> initializerFunction
    ) {

        collectionHandler.registerInitializer(type, initializerFunction);
    }

    /**
     * Registers a custom array initializer using raw/dynamic types.
     * <p>
     * This variant is useful when the component type is not known at compile time.
     * </p>
     *
     * @param componentType       the array component type
     * @param initializerFunction the function that creates new arrays of the specified size
     */
    public <T> void registerDynamicArrayInitializer(
            @NotNull Class<T> componentType,
            @NotNull Function<Integer, T[]> initializerFunction
    ) {
        arrayHandler.registerInitializer(componentType, initializerFunction);
    }

    /**
     * Registers a custom map initializer.
     * <p>
     * This is a convenience method that delegates to {@link MapArgumentTypeHandler}.
     * </p>
     *
     * @param type                the map type
     * @param initializerFunction the supplier that creates new map instances
     * @param <M>                 the map type
     */
    public <M extends Map<?, ?>> void registerMapInitializer(
            @NotNull Class<M> type,
            @NotNull Supplier<M> initializerFunction
    ) {
        mapHandler.registerInitializer(type, initializerFunction);
    }

    /**
     * Resolves an {@link ArgumentType} for the given type.
     * <p>
     * This method iterates through all registered handlers in priority order and
     * returns the first successful resolution.
     * </p>
     *
     * @param type the type to resolve
     * @param <T>  the resolved type
     * @return an optional containing the ArgumentType if found
     */
    public <T> Optional<ArgumentType<S, T>> getResolver(@NotNull Type type) {
        Type boxedType = TypeUtility.primitiveToBoxed(type);
        TypeWrap<?> wrap = TypeWrap.of(boxedType);

        ArgumentTypeLookup<S> lookup = this::getResolver;

        for (ArgumentTypeHandler<S> handler : handlers) {
            if (handler.canHandle(boxedType, wrap)) {
                ArgumentType<S, T> resolved = handler.resolve(boxedType, wrap, lookup);
                if (resolved != null) {
                    return Optional.of(resolved);
                }
            }
        }

        return Optional.empty();
    }


    public ArrayArgumentTypeHandler<S> getArrayHandler() {
        return arrayHandler;
    }

    public CollectionArgumentTypeHandler<S> getCollectionHandler() {
        return collectionHandler;
    }

    public MapArgumentTypeHandler<S> getMapHandler() {
        return mapHandler;
    }
}
