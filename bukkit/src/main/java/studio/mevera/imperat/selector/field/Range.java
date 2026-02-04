package studio.mevera.imperat.selector.field;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class Range<N extends Number> extends Number {

    private final @Nullable N min, max;

    private Range(@Nullable N min, @Nullable N max) {
        this.min = min;
        this.max = max;
    }

    public static <N extends Number> Range<N> atLeast(N numericValue) {
        return of(numericValue, null);
    }

    public static <N extends Number> Range<N> atMost(N numericValue) {
        return of(null, numericValue);
    }

    public static <N extends Number> Range<N> of(@Nullable N min, @Nullable N max) {
        return new Range<>(min, max);
    }

    public @Nullable N getMax() {
        return max;
    }

    public @Nullable N getMin() {
        return min;
    }

    public boolean isInRange(N value) {
        if (value instanceof Range<?> other) {
            return this.equals(other);
        }

        if (min == null) {
            if (max == null) {
                return true;
            }
            return value.doubleValue() <= max.doubleValue();
        } else if (max == null) {
            return value.doubleValue() >= min.doubleValue();
        } else {
            return value.doubleValue() >= min.doubleValue() && value.doubleValue() <= max.doubleValue();
        }
    }

    /**
     * Returns the value of the specified number as an {@code int}.
     *
     * @return the numeric value represented by this object after conversion
     * to type {@code int}.
     */
    @Override
    public int intValue() {
        if (min != null) {
            return min.intValue();
        }
        if (max != null) {
            return max.intValue();
        }
        return 0;
    }

    /**
     * Returns the value of the specified number as a {@code long}.
     *
     * @return the numeric value represented by this object after conversion
     * to type {@code long}.
     */
    @Override
    public long longValue() {
        if (min != null) {
            return min.longValue();
        }
        if (max != null) {
            return max.longValue();
        }
        return 0;
    }

    /**
     * Returns the value of the specified number as a {@code float}.
     *
     * @return the numeric value represented by this object after conversion
     * to type {@code float}.
     */
    @Override
    public float floatValue() {
        if (min != null) {
            return min.floatValue();
        }
        if (max != null) {
            return max.floatValue();
        }
        return 0F;
    }

    /**
     * Returns the value of the specified number as a {@code double}.
     *
     * @return the numeric value represented by this object after conversion
     * to type {@code double}.
     */
    @Override
    public double doubleValue() {
        if (min != null) {
            return min.doubleValue();
        }
        if (max != null) {
            return max.doubleValue();
        }
        return 0D;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Range<?> range)) {
            return false;
        }
        return Objects.equals(min, range.min)
                       && Objects.equals(max, range.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }
}
