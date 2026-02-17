package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A collection that maintains elements sorted by their {@link Priority}.
 * Elements with higher priority values are ordered first.
 *
 * <p>This implementation uses a {@link TreeMap} with a composite key to maintain
 * sorted order automatically when elements are added. Iteration is O(n) without
 * additional sorting overhead.</p>
 *
 * @param <E> the type of elements in this list
 */
public class PriorityList<E> implements Iterable<E> {

    private final TreeMap<PriorityKey, E> map;
    private final Map<E, PriorityKey> elementToKey;
    private final AtomicInteger insertionOrder;

    /**
     * Creates a new empty PriorityList.
     */
    public PriorityList() {
        this.map = new TreeMap<>();
        this.elementToKey = new HashMap<>();
        this.insertionOrder = new AtomicInteger(0);
    }

    /**
     * Adds an element with the specified priority.
     * If the element already exists, it will not be added again.
     *
     * @param priority the priority of the element
     * @param element the element to add
     * @return true if the element was added, false if it already existed
     */
    public boolean add(Priority priority, E element) {
        if (element == null) {
            throw new IllegalArgumentException("Element cannot be null");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority cannot be null");
        }

        if (elementToKey.containsKey(element)) {
            return false;
        }

        int order = insertionOrder.getAndIncrement();
        PriorityKey key = new PriorityKey(priority, order);

        map.put(key, element);
        elementToKey.put(element, key);
        return true;
    }

    /**
     * Adds an element with normal priority.
     *
     * @param element the element to add
     * @return true if the element was added, false if it already existed
     */
    public boolean add(E element) {
        return add(Priority.NORMAL, element);
    }

    /**
     * Removes an element from this list.
     *
     * @param element the element to remove
     * @return true if the element was removed, false if it wasn't present
     */
    public boolean remove(E element) {
        PriorityKey key = elementToKey.remove(element);
        if (key == null) {
            return false;
        }
        map.remove(key);
        return true;
    }

    /**
     * Checks if this list contains the specified element.
     *
     * @param element the element to check for
     * @return true if the element is present
     */
    public boolean contains(E element) {
        return elementToKey.containsKey(element);
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the size of this list
     */
    public int size() {
        return elementToKey.size();
    }

    /**
     * Checks if this list is empty.
     *
     * @return true if this list contains no elements
     */
    public boolean isEmpty() {
        return elementToKey.isEmpty();
    }

    /**
     * Removes all elements from this list.
     */
    public void clear() {
        map.clear();
        elementToKey.clear();
        insertionOrder.set(0);
    }

    /**
     * Returns an iterator over the elements in this list, ordered by priority.
     * Elements with higher priority are returned first.
     * The TreeMap maintains sorted order, so no additional sorting is needed.
     *
     * @return an iterator over the elements
     */
    @NotNull
    @Override
    public Iterator<E> iterator() {
        return map.values().iterator();
    }

    /**
     * Performs the given action for each element in priority order.
     *
     * @param action the action to perform
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        iterator().forEachRemaining(action);
    }

    /**
     * Returns a list of all elements in priority order.
     *
     * @return a new list containing all elements
     */
    public List<E> toList() {
        List<E> result = new ArrayList<>(size());
        forEach(result::add);
        return result;
    }

    /**
     * Returns a sequential {@link Stream} of the elements in this list,
     * ordered by priority. Elements with higher priority are returned first.
     *
     * @return a sequential stream of elements in priority order
     */
    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Returns an immutable view of this PriorityList.
     * The returned list will reflect any changes made to the underlying list,
     * but mutation operations will throw {@link UnsupportedOperationException}.
     *
     * @return an immutable view of this PriorityList
     */
    @NotNull
    public PriorityList<E> asUnmodifiable() {
        return new UnmodifiablePriorityList<>(this);
    }

    @Override
    public String toString() {
        return toList().toString();
    }

    /**
     * Internal key class that combines priority and insertion order.
     * Sorts by priority first (using Priority's natural ordering which is descending),
     * then by insertion order for equal priorities.
     */
    private record PriorityKey(Priority priority, int insertionOrder) implements Comparable<PriorityKey> {

        @Override
        public int compareTo(@NotNull PriorityKey other) {

            // First compare by priority (Priority.compareTo already sorts descending)
            int priorityComp = this.priority.compareTo(other.priority);
            if (priorityComp != 0) {
                return priorityComp;
            }
            // Then by insertion order (ascending - earlier insertions first)
            return Integer.compare(this.insertionOrder, other.insertionOrder);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PriorityKey that)) {
                return false;
            }
            return insertionOrder == that.insertionOrder &&
                           Objects.equals(priority, that.priority);
        }

    }

    /**
     * An immutable wrapper around a PriorityList that prevents modifications.
     * All mutation operations throw {@link UnsupportedOperationException}.
     */
    private static class UnmodifiablePriorityList<E> extends PriorityList<E> {

        private final PriorityList<E> delegate;

        UnmodifiablePriorityList(PriorityList<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean add(Priority priority, E element) {
            throw new UnsupportedOperationException("Cannot modify an unmodifiable PriorityList");
        }

        @Override
        public boolean add(E element) {
            throw new UnsupportedOperationException("Cannot modify an unmodifiable PriorityList");
        }

        @Override
        public boolean remove(E element) {
            throw new UnsupportedOperationException("Cannot modify an unmodifiable PriorityList");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Cannot modify an unmodifiable PriorityList");
        }

        @Override
        public boolean contains(E element) {
            return delegate.contains(element);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @NotNull
        @Override
        public Iterator<E> iterator() {
            Iterator<E> it = delegate.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Cannot remove from an unmodifiable PriorityList");
                }
            };
        }

        @Override
        public List<E> toList() {
            return delegate.toList();
        }

        @Override
        public Stream<E> stream() {
            return delegate.stream();
        }

        @NotNull
        @Override
        public PriorityList<E> asUnmodifiable() {
            return this;
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
