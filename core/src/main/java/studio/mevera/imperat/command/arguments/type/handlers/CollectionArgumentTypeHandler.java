package studio.mevera.imperat.command.arguments.type.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeHandler;
import studio.mevera.imperat.command.arguments.type.ArgumentTypeLookup;
import studio.mevera.imperat.command.arguments.type.CollectionArgument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.priority.Priority;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;
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
public final class CollectionArgumentTypeHandler<S extends CommandSource> implements ArgumentTypeHandler<S> {

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
    public @NotNull Priority getPriority() {
        return Priority.NORMAL;
    }
}
