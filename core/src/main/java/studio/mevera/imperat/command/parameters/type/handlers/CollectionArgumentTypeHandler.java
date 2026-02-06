package studio.mevera.imperat.command.parameters.type.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.parameters.type.CollectionArgument;
import studio.mevera.imperat.command.parameters.type.ArgumentTypeLookup;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.util.Priority;
import studio.mevera.imperat.util.TypeWrap;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Handler for resolving {@link ArgumentType} instances for {@link Collection} types.
 * <p>
 * This handler supports all standard Java collection implementations and allows
 * registration of custom collection initializers.
 * </p>
 *
 * @param <S> the source type
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class CollectionArgumentTypeHandler<S extends Source> implements ArgumentTypeHandler<S> {

    private final Map<Type, Supplier<Collection<?>>> initializers = new LinkedHashMap<>();

    /**
     * Creates a new CollectionArgumentTypeHandler with default initializers.
     */
    public CollectionArgumentTypeHandler() {
        // List implementations
        initializers.put(ArrayList.class, ArrayList::new);
        initializers.put(LinkedList.class, LinkedList::new);
        initializers.put(Vector.class, Vector::new);
        initializers.put(Stack.class, Stack::new);
        initializers.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);

        // Set implementations
        initializers.put(HashSet.class, HashSet::new);
        initializers.put(LinkedHashSet.class, LinkedHashSet::new);
        initializers.put(TreeSet.class, TreeSet::new);
        initializers.put(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new);
        initializers.put(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new);

        // Queue/Deque implementations
        initializers.put(PriorityQueue.class, PriorityQueue::new);
        initializers.put(ArrayDeque.class, ArrayDeque::new);
        initializers.put(ConcurrentLinkedQueue.class, ConcurrentLinkedQueue::new);
        initializers.put(ConcurrentLinkedDeque.class, ConcurrentLinkedDeque::new);
        initializers.put(LinkedBlockingQueue.class, LinkedBlockingQueue::new);
        initializers.put(PriorityBlockingQueue.class, PriorityBlockingQueue::new);
        initializers.put(DelayQueue.class, DelayQueue::new);
        initializers.put(SynchronousQueue.class, SynchronousQueue::new);
        initializers.put(LinkedTransferQueue.class, LinkedTransferQueue::new);

        // Interface defaults (lowest specificity, checked last)
        initializers.put(List.class, ArrayList::new);
        initializers.put(Set.class, HashSet::new);
        initializers.put(Queue.class, LinkedList::new);
        initializers.put(Deque.class, ArrayDeque::new);
        initializers.put(Collection.class, ArrayList::new);
    }

    /**
     * Registers a custom collection initializer.
     *
     * @param type     the collection type
     * @param supplier the supplier that creates new instances
     * @param <C>      the collection type
     */
    public <C extends Collection<?>> void registerInitializer(@NotNull Class<C> type, @NotNull Supplier<C> supplier) {
        initializers.put(type, (Supplier<Collection<?>>) supplier);
    }

    @Override
    public boolean canHandle(@NotNull Type type, @NotNull TypeWrap<?> wrap) {
        return wrap.isSubtypeOf(Collection.class);
    }

    @Override
    public @Nullable <T> ArgumentType<S, T> resolve(
            @NotNull Type type,
            @NotNull TypeWrap<?> wrap,
            @NotNull ArgumentTypeLookup<S> lookup
    ) {
        var parameterizedTypes = wrap.getParameterizedTypes();
        if (parameterizedTypes == null || parameterizedTypes.length == 0) {
            throw new IllegalArgumentException("Raw collection types are not allowed");
        }

        TypeWrap<?> componentType = TypeWrap.of(parameterizedTypes[0]);
        ArgumentType<S, ?> componentResolver = lookup.lookupOrThrow(componentType.getType());

        Supplier<Collection<?>> initializer = findInitializer(wrap);

        @SuppressWarnings("unchecked")
        Supplier<Collection<Object>> typedInitializer = (Supplier<Collection<Object>>) (Supplier<?>) initializer;

        return (ArgumentType<S, T>) new CollectionArgument<>(
                (TypeWrap<Collection<Object>>) wrap,
                typedInitializer,
                (ArgumentType<S, Object>) componentResolver
        );
    }

    private Supplier<Collection<?>> findInitializer(TypeWrap<?> wrap) {
        Type rawType = wrap.getRawType();

        // Direct match
        Supplier<Collection<?>> direct = initializers.get(rawType);
        if (direct != null) {
            return direct;
        }

        // Search for compatible supertype
        for (var entry : initializers.entrySet()) {
            if (TypeWrap.of(rawType).isSupertypeOf(entry.getKey())) {
                return entry.getValue();
            }
        }

        throw new IllegalArgumentException("Unknown collection type: " + rawType.getTypeName());
    }

    @Override
    public @NotNull Priority priority() {
        return Priority.NORMAL;
    }
}
