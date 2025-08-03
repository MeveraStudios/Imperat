package studio.mevera.imperat.command.parameters;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.NumericComparator;
import studio.mevera.imperat.context.Source;

/**
 * Represents a behavior that deals with numeric
 * inputs if they are ranged from min to max using {@link NumericRange}
 */
public interface NumericParameter<S extends Source> extends CommandParameter<S> {

    /**
     * @return The actual range of the numeric parameter
     * returns null if no range is specified!
     */
    @Nullable
    NumericRange getRange();

    default boolean hasRange() {
        return getRange() != null;
    }

    default <N extends Number> boolean matchesRange(N value) {
        var range = getRange();
        return range != null && NumericComparator.of(value).isWithin(value, range);
    }

}
