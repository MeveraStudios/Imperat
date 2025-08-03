package studio.mevera.imperat.command;

import studio.mevera.imperat.command.parameters.NumericRange;
import studio.mevera.imperat.util.Preconditions;

import java.util.Map;

public interface NumericComparator<N extends Number> {

    NumericComparator<Double> DOUBLE_NUMERIC_COMPARATOR = (number, range) -> range.matches(number);

    NumericComparator<Float> FLOAT_NUMERIC_COMPARATOR = (number, range) -> range.matches(number);

    NumericComparator<Integer> INTEGER_NUMERIC_COMPARATOR = (number, range) -> range.matches(number);

    NumericComparator<Long> LONG_NUMERIC_COMPARATOR = (number, range) -> range.matches((double) ((long) number));


    Map<Class<? extends Number>, NumericComparator<?>> COMPARATORS = Map.of(Double.class, DOUBLE_NUMERIC_COMPARATOR,
        Float.class, FLOAT_NUMERIC_COMPARATOR,
        Integer.class, INTEGER_NUMERIC_COMPARATOR,
        Long.class, LONG_NUMERIC_COMPARATOR
    );

    @SuppressWarnings("unchecked")
    static <N extends Number> NumericComparator<N> of(N value) {
        Preconditions.notNull(value, "Value");
        return (NumericComparator<N>) COMPARATORS.getOrDefault(value.getClass(), DOUBLE_NUMERIC_COMPARATOR);
    }

    boolean isWithin(N number, NumericRange range);
}
