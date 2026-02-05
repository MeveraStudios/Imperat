package studio.mevera.imperat.context;
import studio.mevera.imperat.command.parameters.Either;
import studio.mevera.imperat.command.parameters.type.ArgumentType;

import org.jetbrains.annotations.ApiStatus;
import studio.mevera.imperat.command.parameters.type.ArrayArgument;
import studio.mevera.imperat.command.parameters.type.CollectionArgument;
import studio.mevera.imperat.command.parameters.type.CompletableFutureArgument;
import studio.mevera.imperat.command.parameters.type.EitherArgument;
import studio.mevera.imperat.command.parameters.type.EnumArgument;
import studio.mevera.imperat.command.parameters.type.MapArgument;
import studio.mevera.imperat.command.parameters.type.OptionalArgument;
import studio.mevera.imperat.command.parameters.type.ArgumentTypes;
import studio.mevera.imperat.util.Registry;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.function.Function;
import java.util.function.Supplier;

@ApiStatus.Internal
@SuppressWarnings({"rawtypes", "unchecked"})
public final class ArgumentTypeRegistry<S extends Source> extends Registry<Type, Supplier<ArgumentType>> {

    private final Registry<Type, Supplier<Collection<?>>> collectionInitializer = new Registry<>(LinkedHashMap::new);
    private final Registry<Type, Function<Integer, Object[]>> arrayInitializer = new Registry<>(LinkedHashMap::new);
    private final Registry<Type, Supplier<Map<?, ?>>> mapInitializer = new Registry<>(LinkedHashMap::new);

    {
        // List implementations
        collectionInitializer.setData(ArrayList.class, ArrayList::new);
        collectionInitializer.setData(LinkedList.class, LinkedList::new);
        collectionInitializer.setData(Vector.class, Vector::new);
        collectionInitializer.setData(Stack.class, Stack::new);
        collectionInitializer.setData(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);

        // Set implementations
        collectionInitializer.setData(HashSet.class, HashSet::new);
        collectionInitializer.setData(LinkedHashSet.class, LinkedHashSet::new);
        collectionInitializer.setData(TreeSet.class, TreeSet::new);
        collectionInitializer.setData(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new);
        collectionInitializer.setData(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new);

        // Queue/Deque implementations
        collectionInitializer.setData(PriorityQueue.class, PriorityQueue::new);
        collectionInitializer.setData(ArrayDeque.class, ArrayDeque::new);
        collectionInitializer.setData(ConcurrentLinkedQueue.class, ConcurrentLinkedQueue::new);
        collectionInitializer.setData(ConcurrentLinkedDeque.class, ConcurrentLinkedDeque::new);
        collectionInitializer.setData(LinkedBlockingQueue.class, LinkedBlockingQueue::new);
        collectionInitializer.setData(PriorityBlockingQueue.class, PriorityBlockingQueue::new);
        collectionInitializer.setData(DelayQueue.class, DelayQueue::new);
        collectionInitializer.setData(SynchronousQueue.class, SynchronousQueue::new);
        collectionInitializer.setData(LinkedTransferQueue.class, LinkedTransferQueue::new);
    }

    {
        // Wrapped types array initializers with size parameter
        arrayInitializer.setData(Boolean.class, Boolean[]::new);
        arrayInitializer.setData(Byte.class, Byte[]::new);
        arrayInitializer.setData(Short.class, Short[]::new);
        arrayInitializer.setData(Integer.class, Integer[]::new);
        arrayInitializer.setData(Long.class, Long[]::new);
        arrayInitializer.setData(Float.class, Float[]::new);
        arrayInitializer.setData(Double.class, Double[]::new);
        arrayInitializer.setData(Character.class, Character[]::new);
        arrayInitializer.setData(String.class, String[]::new);
    }

    {

        // Standard Map Implementations
        mapInitializer.setData(HashMap.class, HashMap::new);
        mapInitializer.setData(LinkedHashMap.class, LinkedHashMap::new);
        mapInitializer.setData(TreeMap.class, TreeMap::new);
        mapInitializer.setData(WeakHashMap.class, WeakHashMap::new);
        mapInitializer.setData(IdentityHashMap.class, IdentityHashMap::new);

        // Concurrent Map Implementations
        mapInitializer.setData(ConcurrentHashMap.class, ConcurrentHashMap::new);
        mapInitializer.setData(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new);

        // Specialized Map Types
        mapInitializer.setData(EnumMap.class, () -> {
            throw new UnsupportedOperationException("EnumMap requires an enum type parameter");
        });

        // Sorted Map Interfaces
        mapInitializer.setData(SortedMap.class, TreeMap::new);
        mapInitializer.setData(NavigableMap.class, TreeMap::new);

    }

    private ArgumentTypeRegistry() {
        super();
        registerResolver(Boolean.class, ArgumentTypes::bool);
        registerResolver(String.class, ArgumentTypes::string);
        registerResolver(UUID.class, ArgumentTypes::uuid);
    }

    public static <S extends Source> ArgumentTypeRegistry<S> createDefault() {
        return new ArgumentTypeRegistry<>();
    }

    public void registerResolver(Type type, Supplier<ArgumentType> resolver) {
        setData(type, resolver);
    }

    <E, C extends Collection<E>> Supplier<C> initializeNewCollection(TypeWrap<?> fullType) {
        var collectionType = fullType.getRawType();
        var data = collectionInitializer.getData(collectionType);
        return data.map(collectionSupplier -> (Supplier<C>) collectionSupplier)
                       .orElseGet(
                               () -> (Supplier<C>) collectionInitializer.search((ctype, supplier) -> TypeWrap.of(collectionType).isSupertypeOf(ctype))
                                                           .orElseThrow(() -> new IllegalArgumentException(
                                                                   "Unknown collection-type detected '" + collectionType.getTypeName() + "'")));
    }

    Function<Integer, Object[]> initializeNewArray(TypeWrap<?> componentType) {
        var data = arrayInitializer.getData(componentType.getType());
        if (data.isEmpty()) {

            Function<Integer, Object[]> func = null;
            for (Type key : arrayInitializer.getKeys()) {
                if (componentType.isSupertypeOf(key)) {
                    func = arrayInitializer.getData(key).orElse(null);
                    if (func != null) {
                        break;
                    }
                }
            }
            if (func == null) {
                throw new IllegalArgumentException("Unknown array-type detected '" + componentType.getType().getTypeName() + "'");
            }
            return func;
        } else {
            return data.get();
        }
    }

    <K, V, M extends Map<K, V>> Supplier<M> initializeNewMap(TypeWrap<?> fullType) {
        Type mapRawType = fullType.getRawType();
        var initializer = mapInitializer.getData(mapRawType);
        return initializer.map(mapSupplier -> (Supplier<M>) mapSupplier)
                       .orElseGet(() -> (Supplier<M>) mapInitializer.search((ctype, supplier) -> TypeWrap.of(mapRawType).isSupertypeOf(ctype))
                                                              .orElseThrow(() -> new IllegalArgumentException(
                                                                      "Unknown map-type detected '" + mapRawType.getTypeName() + "'")));
    }

    private <E, C extends Collection<E>> CollectionArgument<S, E, C> getCollectionResolver(TypeWrap<?> type) {
        var parameterizedTypes = type.getParameterizedTypes();
        if (parameterizedTypes == null) {
            throw new IllegalArgumentException("NULL PARAMETERIZED TYPES");
        }
        TypeWrap<E> componentType = (TypeWrap<E>) TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, E> componentResolver = (ArgumentType<S, E>) getResolver(componentType.getType()).orElseThrow(
                () -> new IllegalArgumentException("Unknown component-type detected '" + componentType.getType().getTypeName() + "'"));
        return new CollectionArgument<>((TypeWrap<C>) type, initializeNewCollection(type), componentResolver);
    }

    private <E> ArgumentType<S, E[]> getArrayResolver(TypeWrap<?> type) {
        var componentType = type.getComponentType();
        if (componentType == null) {
            throw new IllegalArgumentException("NULL COMPONENT TYPE");
        }
        ArgumentType<S, E> componentResolver = (ArgumentType<S, E>) getResolver(componentType.getType()).orElseThrow(
                () -> new IllegalArgumentException("Unknown component-type detected '" + componentType.getType().getTypeName() + "'"));
        return new ArrayArgument<>((TypeWrap<E[]>) type, initializeNewArray(componentType), componentResolver) {
        };
    }

    private <K, V, M extends Map<K, V>> MapArgument<S, K, V, M> getMapResolver(TypeWrap<?> type) {
        var parameterizedTypes = type.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw types are not allowed as parameters !");
        }
        TypeWrap<K> keyType = (TypeWrap<K>) TypeWrap.of(parameterizedTypes[0]);
        TypeWrap<V> valueType = (TypeWrap<V>) TypeWrap.of(parameterizedTypes[0]);

        ArgumentType<S, K> keyResolver = (ArgumentType<S, K>) getResolver(keyType.getType()).orElseThrow(
                () -> new IllegalArgumentException("Unknown component-type detected '" + keyType.getType().getTypeName() + "'"));
        ArgumentType<S, V> valueResolver = (ArgumentType<S, V>) getResolver(valueType.getType()).orElseThrow(
                () -> new IllegalArgumentException("Unknown component-type detected '" + valueType.getType().getTypeName() + "'"));

        return new MapArgument<>((TypeWrap<M>) type, initializeNewMap(type), keyResolver, valueResolver);
    }

    private <T> CompletableFutureArgument<S, T> getFutureResolver(TypeWrap<?> type) {
        var parameterizedTypes = type.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw types are not allowed as parameters !");
        }
        TypeWrap<T> futureTypeInput = (TypeWrap<T>) TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, T> futureTypeResolver =
                (ArgumentType<S, T>) getResolver(futureTypeInput.getType()).orElseThrow(() -> new IllegalArgumentException("Unknown "
                                                                                                                                    + "component"
                                                                                                                                    + "-type "
                                                                                                                                    + "detected '"
                                                                                                                                    + futureTypeInput.getType()
                                                                                                                                              .getTypeName()
                                                                                                                                    + "'"));

        return ArgumentTypes.future((TypeWrap<CompletableFuture<T>>) type, futureTypeResolver);
    }

    private <T> OptionalArgument<S, T> getOptionalResolver(TypeWrap<?> type) {
        var parameterizedTypes = type.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw types are not allowed as parameters !");
        }
        TypeWrap<T> optionalType = (TypeWrap<T>) TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, T> optionalTypeResolver =
                (ArgumentType<S, T>) getResolver(optionalType.getType()).orElseThrow(() ->
                      new IllegalArgumentException("Unknown " + "component-type "
                                                           + "detected '" + optionalType.getType().getTypeName()
                                                                                                                                 + "'"));
        return ArgumentTypes.optional((TypeWrap<Optional<T>>) type, optionalTypeResolver);
    }
    private <A, B> EitherArgument<S, A, B> getEitherResolver(TypeWrap<?> type) {
        var parameterizedTypes = type.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw types are not allowed as parameters !");
        }
        TypeWrap<A> aType = (TypeWrap<A>) TypeWrap.of(parameterizedTypes[0]);
        TypeWrap<B> bType = (TypeWrap<B>) TypeWrap.of(parameterizedTypes[1]);
        return ArgumentTypes.either((TypeWrap<Either<A,B>>) type, aType, bType);
    }

    public <C extends Collection<?>> void registerCollectionInitializer(Class<C> type, Supplier<C> initializerFunction) {
        collectionInitializer.setData(type, (Supplier<Collection<?>>) initializerFunction);
    }

    public <ArrayComponent> void registerArrayInitializer(Class<ArrayComponent> type, Function<Integer, Object[]> initializerFunction) {
        var sample = initializerFunction.apply(0);
        if (!TypeUtility.matches(sample.getClass().getComponentType(), type)) {
            throw new IllegalArgumentException(
                    "Array initializer type '%s' does not match '%s'".formatted(type.getName(), sample.getClass().getComponentType()));
        }
        arrayInitializer.setData(type, initializerFunction);
    }

    public <M extends Map<?, ?>> void registerMapInitializer(Class<M> type, Supplier<M> initializerFunction) {
        mapInitializer.setData(type, (Supplier<Map<?, ?>>) initializerFunction);
    }

    public <T> Optional<ArgumentType<S, T>> getResolver(Type type) {
        return
                Optional.ofNullable(getData(TypeUtility.primitiveToBoxed(type))
                                            .map(Supplier::get)
                                            .orElseGet(() -> {
                                                //TODO add handler architechture instead of this ugly if else
                                                var wrap = TypeWrap.of(type);
                                                if (wrap.isArray()) {
                                                    //array type
                                                    return getArrayResolver(wrap);

                                                } else if (wrap.isSubtypeOf(Collection.class)) {
                                                    //collection type
                                                    return this.getCollectionResolver(wrap);
                                                } else if (wrap.isSubtypeOf(Map.class)) {
                                                    //map type
                                                    return this.getMapResolver(wrap);
                                                } else if (wrap.getRawType().equals(CompletableFuture.class)) {
                                                    return this.getFutureResolver(wrap);
                                                } else if (wrap.getRawType().equals(Optional.class)) {
                                                    return this.getOptionalResolver(wrap);
                                                }else if(wrap.getRawType().equals(Either.class)) {
                                                    return this.getEitherResolver(wrap);
                                                } else if (TypeUtility.isNumericType(wrap)) {
                                                    return ArgumentTypes.numeric((Class<? extends Number>) type);
                                                } else if (TypeUtility.areRelatedTypes(type, Enum.class)) {
                                                    return new EnumArgument<>((TypeWrap<Enum<?>>) TypeWrap.of(type));
                                                }

                                                for (var registeredType : getKeys()) {
                                                    if (TypeUtility.areRelatedTypes(type, registeredType)) {
                                                        return getData(registeredType).map((s) -> ((ArgumentType<S, T>) s.get())).orElse(null);
                                                    }
                                                }
                                                return null;
                                            }));
    }



}
