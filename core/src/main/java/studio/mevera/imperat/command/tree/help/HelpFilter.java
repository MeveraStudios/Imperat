package studio.mevera.imperat.command.tree.help;

import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

/**
 * A functional interface for filtering help entries during query operations.
 * Filters are applied to {@link CommandPathway}s to determine which usages
 * should be included in help results.
 *
 * <p>Filters can be chained together to create complex filtering logic,
 * such as permission checks, visibility rules, or category filtering.</p>
 *
 * @param <S> the source type
 * @author Mqzen
 */
@FunctionalInterface
public interface HelpFilter<S extends CommandSource> {

    /**
     * Determines whether a pathway should be included in help results.
     *
     * @param pathway the command pathway to evaluate
     * @return {@code true} if the pathway passes the filter and should be included,
     *         {@code false} if it should be excluded
     */
    boolean filter(CommandPathway<S> pathway);

    /**
     * Creates a composite filter that requires both this and another filter to pass.
     *
     * @param other the other filter to combine with
     * @return a new filter that passes only if both filters pass
     */
    default HelpFilter<S> and(HelpFilter<S> other) {
        return pathway -> this.filter(pathway) && other.filter(pathway);
    }

    /**
     * Creates a composite filter that passes if either this or another filter passes.
     *
     * @param other the other filter to combine with
     * @return a new filter that passes if either filter passes
     */
    default HelpFilter<S> or(HelpFilter<S> other) {
        return pathway -> this.filter(pathway) || other.filter(pathway);
    }

    /**
     * Creates a filter that inverts this filter's result.
     *
     * @return a new filter with inverted logic
     */
    default HelpFilter<S> negate() {
        return pathway -> !this.filter(pathway);
    }
}