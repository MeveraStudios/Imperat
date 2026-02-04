package studio.mevera.imperat.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents the relative priority used when resolving command parameters.
 * <p>
 * A {@code Priority} is an immutable wrapper around an integer level. Higher
 * level values represent higher precedence when multiple parameter handlers,
 * resolvers, or strategies are applicable. The exact comparison is typically
 * numeric: a handler with a higher {@linkplain #getLevel() level} is preferred
 * over one with a lower level.
 * </p>
 *
 * <p>
 * This class provides a set of common baseline priorities:
 * </p>
 * <ul>
 *     <li>{@link #MINIMUM} – a sentinel priority intended for fallbacks or
 *     handlers that should only be used when nothing else matches.</li>
 *     <li>{@link #LOW} – a default low priority for generic or less specific
 *     handlers.</li>
 *     <li>{@link #NORMAL} – the standard priority for most parameter
 *     handlers.</li>
 *     <li>{@link #HIGH} – a higher priority for more specific or preferred
 *     handlers.</li>
 *     <li>{@link #MAXIMUM} – a sentinel priority for handlers that should
 *     win over all others when applicable.</li>
 * </ul>
 *
 * <p>
 * Custom priorities can be created via {@link #of(int)} or by adjusting an
 * existing instance with {@link #plus(int)}. When defining custom levels,
 * choose values relative to the built-in constants so that the resulting
 * ordering remains clear and predictable.
 * </p>
 */
public final class Priority implements Comparable<Priority> {
    public static final Priority MINIMUM = new Priority(Integer.MIN_VALUE);
    public static final Priority LOW = new Priority(0);
    public static final Priority NORMAL = new Priority(20);
    public static final Priority HIGH = new Priority(100);
    public static final Priority MAXIMUM = new Priority(Integer.MAX_VALUE);

    private final int level;

    public static Priority of(int level) {
        return new Priority(level);
    }

    private Priority(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public Priority plus(int n) {
        return new Priority(this.level + n);
    }

    @Override
    public int compareTo(@NotNull Priority o) {
        return Integer.compare(o.level, this.level);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Priority priority)) {
            return false;
        }
        return level == priority.level;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(level);
    }
}