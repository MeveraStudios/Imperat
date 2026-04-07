package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

@ApiStatus.Internal
final class ArgumentInputImpl extends ArrayList<String> implements ArgumentInput {

    private final String originalRaw;

    ArgumentInputImpl(String originalRaw, @NotNull Collection<? extends String> input) {
        super(input);
        this.originalRaw = originalRaw;
    }


    ArgumentInputImpl(String originalRaw, @NotNull String... rawArgs) {
        super(rawArgs.length);
        this.originalRaw = originalRaw;
        this.addAll(Arrays.asList(rawArgs));
    }

    ArgumentInputImpl() {
        super();
        this.originalRaw = "";
    }

    @Override
    public String getOriginalRaw() {
        return originalRaw;
    }


    @Override
    public @NotNull String join(String delimiter) {
        return String.join(delimiter, this);
    }

    @Override
    public @NotNull String join(@NotNull String delimiter, int startIndex) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (int i = startIndex; i < this.size(); i++) {
            joiner.add(get(i));
        }
        return joiner.toString();
    }


    @Override
    public @NotNull ArgumentInput copy() {
        return new ArgumentInputImpl(originalRaw, this);
    }

    @Override
    public void addFirst(String s) {
        add(0, s);
    }

    @Override
    public void addLast(String s) {
        add(s);
    }

    @Override
    public boolean offerFirst(String s) {
        addFirst(s);
        return true;
    }

    @Override
    public boolean offerLast(String s) {
        return add(s);
    }

    @Override
    public String removeFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(0);
    }

    @Override
    public String removeLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return remove(size() - 1);
    }

    @Override
    public String pollFirst() {
        return isEmpty() ? null : remove(0);
    }

    @Override
    public String pollLast() {
        return isEmpty() ? null : remove(size() - 1);
    }

    @Override
    public String getFirst() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(0);
    }

    @Override
    public String getLast() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return get(size() - 1);
    }

    @Override
    public String peekFirst() {
        return isEmpty() ? null : get(0);
    }

    @Override
    public String peekLast() {
        return isEmpty() ? null : get(size() - 1);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        for (int i = size() - 1; i >= 0; i--) {
            if (java.util.Objects.equals(get(i), o)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean offer(String s) {
        return add(s);
    }

    @Override
    public String remove() {
        return removeFirst();
    }

    @Override
    public String poll() {
        return pollFirst();
    }

    @Override
    public String element() {
        return getFirst();
    }

    @Override
    public String peek() {
        return peekFirst();
    }

    @Override
    public void push(String s) {
        addFirst(s);
    }

    @Override
    public String pop() {
        return removeFirst();
    }

    @Override
    public Iterator<String> descendingIterator() {
        return new Iterator<>() {
            private int index = size() - 1;

            @Override
            public boolean hasNext() {
                return index >= 0;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return get(index--);
            }
        };
    }

}
