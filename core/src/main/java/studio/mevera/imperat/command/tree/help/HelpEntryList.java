package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.context.Source;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A custom list implementation that maintains insertion order, <strong>prevents duplicates</strong>,
 * and supports indexing - implemented without using any Java collections.
 * 
 * <p>Uses a dynamically resizing array with linear search for duplicate detection.
 * Optimized for small to medium-sized lists (typical for help entries).</p>
 * 
 * @param <S> the source type
 * @author Mqzen
 */
public final class HelpEntryList<S extends Source> implements Iterable<HelpEntry<S>> {
    
    private static final int DEFAULT_CAPACITY = 16;
    private static final float GROWTH_FACTOR = 1.5f;
    
    private HelpEntry<S>[] elements;
    private int size;
    
    private final static HelpEntryList<?> EMPTY_HELP_LIST = new HelpEntryList<>();
    
    public static <S extends Source> HelpEntryList<S> empty() {
        return (HelpEntryList<S>) EMPTY_HELP_LIST;
    }
    
    /**
     * Creates a new HelpEntryList with default initial capacity.
     */
    @SuppressWarnings("unchecked")
    public HelpEntryList() {
        this.elements = (HelpEntry<S>[]) new HelpEntry[DEFAULT_CAPACITY];
        this.size = 0;
    }
    
    /**
     * Creates a new HelpEntryList with specified initial capacity.
     * 
     * @param initialCapacity the initial capacity
     */
    @SuppressWarnings("unchecked")
    public HelpEntryList(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be positive");
        }
        this.elements = (HelpEntry<S>[]) new HelpEntry[initialCapacity];
        this.size = 0;
    }
    
    /**
     * Adds an entry if it's not already present.
     * 
     * @param entry the entry to add
     * @return {@code true} if the entry was added, {@code false} if it was a duplicate
     */
    public boolean add(HelpEntry<S> entry) {
        if (entry == null) {
            throw new NullPointerException("Cannot add null entry");
        }
        
        // Check for duplicate - O(n)
        if (containsInternal(entry)) {
            return false;
        }
        
        // Ensure capacity
        ensureCapacity(size + 1);
        
        // Add element
        elements[size++] = entry;
        return true;
    }
    
    /**
     * Fast internal contains check using direct array iteration.
     */
    private boolean containsInternal(HelpEntry<S> entry) {
        // Optimized loop for better CPU cache usage
        for (int i = 0; i < size; i++) {
            if (elements[i].equals(entry)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Ensures the array has enough capacity, resizing if necessary.
     */
    @SuppressWarnings("unchecked")
    private void ensureCapacity(int minCapacity) {
        if (minCapacity > elements.length) {
            // Calculate new capacity
            int newCapacity = (int) (elements.length * GROWTH_FACTOR);
            if (newCapacity < minCapacity) {
                newCapacity = minCapacity;
            }
            
            // Create new array and copy elements
            HelpEntry<S>[] newElements = (HelpEntry<S>[]) new HelpEntry[newCapacity];
            System.arraycopy(elements, 0, newElements, 0, size);
            elements = newElements;
        }
    }
    
    /**
     * Gets an entry by index.
     * 
     * @param index the index
     * @return the entry at the specified index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public HelpEntry<S> get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        return elements[index];
    }
    
    /**
     * Returns the number of entries.
     * 
     * @return the size of the list
     */
    public int size() {
        return size;
    }
    
    /**
     * Checks if the list is empty.
     * 
     * @return {@code true} if empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Checks if an entry exists in the list.
     * 
     * @param entry the entry to check
     * @return {@code true} if the entry exists, {@code false} otherwise
     */
    public boolean contains(HelpEntry<S> entry) {
        if (entry == null) {
            return false;
        }
        return containsInternal(entry);
    }
    
    /**
     * Clears all entries.
     */
    public void clear() {
        // Help GC by nulling references
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }
    
    /**
     * Returns the index of an entry.
     * 
     * @param entry the entry to find
     * @return the index of the entry, or -1 if not found
     */
    public int indexOf(HelpEntry<S> entry) {
        if (entry == null) {
            return -1;
        }
        
        for (int i = 0; i < size; i++) {
            if (elements[i].equals(entry)) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Removes an entry by index.
     * 
     * @param index the index to remove
     * @return the removed entry
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public HelpEntry<S> remove(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        
        HelpEntry<S> removed = elements[index];
        
        // Shift elements left
        int numToMove = size - index - 1;
        if (numToMove > 0) {
            System.arraycopy(elements, index + 1, elements, index, numToMove);
        }
        
        // Clear last element and decrement size
        elements[--size] = null;
        
        return removed;
    }
    
    /**
     * Removes a specific entry.
     * 
     * @param entry the entry to remove
     * @return {@code true} if the entry was removed, {@code false} if not found
     */
    public boolean remove(HelpEntry<S> entry) {
        if (entry == null) {
            return false;
        }
        
        int index = indexOf(entry);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }
    
    /**
     * Converts to an array.
     * 
     * @return an array containing all entries
     */
    @SuppressWarnings("unchecked")
    public HelpEntry<S>[] toArray() {
        HelpEntry<S>[] result = (HelpEntry<S>[]) new HelpEntry[size];
        System.arraycopy(elements, 0, result, 0, size);
        return result;
    }
    
    /**
     * Trims the internal capacity to the current size.
     * Call this after adding all elements to save memory.
     */
    @SuppressWarnings("unchecked")
    public void trimToSize() {
        if (size < elements.length) {
            HelpEntry<S>[] trimmed = (HelpEntry<S>[]) new HelpEntry[size];
            System.arraycopy(elements, 0, trimmed, 0, size);
            elements = trimmed;
        }
    }
    
    /**
     * Creates a copy of a range of elements.
     * 
     * @param fromIndex low endpoint (inclusive)
     * @param toIndex high endpoint (exclusive)
     * @return a new HelpEntryList containing the specified range
     */
    public HelpEntryList<S> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        
        HelpEntryList<S> result = new HelpEntryList<>(toIndex - fromIndex);
        for (int i = fromIndex; i < toIndex; i++) {
            result.add(elements[i]); // Will check for duplicates
        }
        return result;
    }
    
    /**
     * Adds all entries from another HelpEntryList.
     * 
     * @param other the other list
     * @return the number of entries actually added (excluding duplicates)
     */
    public int addAll(HelpEntryList<S> other) {
        if (other == null || other.isEmpty()) {
            return 0;
        }
        
        int added = 0;
        // Ensure we have capacity for worst case
        ensureCapacity(size + other.size);
        
        for (int i = 0; i < other.size; i++) {
            if (add(other.elements[i])) {
                added++;
            }
        }
        return added;
    }
    
    /**
     * Returns the current capacity of the internal array.
     * 
     * @return the current capacity
     */
    public int capacity() {
        return elements.length;
    }
    
    @Override
    public Iterator<HelpEntry<S>> iterator() {
        return new HelpEntryIterator();
    }
    
    /**
     * Custom iterator implementation.
     */
    private class HelpEntryIterator implements Iterator<HelpEntry<S>> {
        private int cursor = 0;
        private int lastRet = -1;
        
        @Override
        public boolean hasNext() {
            return cursor < size;
        }
        
        @Override
        public HelpEntry<S> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastRet = cursor;
            return elements[cursor++];
        }
        
        @Override
        public void remove() {
            if (lastRet < 0) {
                throw new IllegalStateException();
            }
            HelpEntryList.this.remove(lastRet);
            cursor = lastRet;
            lastRet = -1;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HelpEntryList<?> that)) return false;
        
        if (this.size != that.size) return false;
        
        for (int i = 0; i < size; i++) {
            if (!elements[i].equals(that.elements[i])) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size; i++) {
            result = 31 * result + (elements[i] == null ? 0 : elements[i].hashCode());
        }
        return result;
    }
    
    @Override
    public String toString() {
        if (size == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < size; i++) {
            sb.append(elements[i]);
            if (i < size - 1) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }
    
    /**
     * Converts to a Java Set (LinkedHashSet).
     * This is the only method that uses Java collections for API compatibility.
     * 
     * @return a LinkedHashSet containing all entries in order
     */
    public java.util.Set<HelpEntry<S>> toSet() {
        java.util.LinkedHashSet<HelpEntry<S>> set = new java.util.LinkedHashSet<>(size);
        set.addAll(Arrays.asList(elements).subList(0, size));
        return set;
    }
}