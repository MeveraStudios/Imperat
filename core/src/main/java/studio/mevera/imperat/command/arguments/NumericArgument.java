package studio.mevera.imperat.command.arguments;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.NumericComparator;
import studio.mevera.imperat.context.CommandSource;

/**
 * Represents a behavior that deals with numeric
 * inputs if they are ranged from min to max using {@link NumericRange}
 */
public interface NumericArgument<S extends CommandSource> extends Argument<S> {

    /**
     * @return The actual range of the numeric parameter
     * returns null if no range is specified!
     */
    @NotNull
    NumericRange getRange();

    default boolean hasRange() {
        return !getRange().isEmpty();
    }

    default <N extends Number> boolean matchesRange(N value) {
        var range = getRange();
        return NumericComparator.of(value).isWithin(value, range);
    }

}
