package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * An immutable data class representing a query for help documentation.
 * <p>
 * This class encapsulates the various parameters that can be used to filter and
 * limit help entries, such as maximum depth, a result limit, and a list of
 * specific filters. It is designed to be constructed using its nested {@link Builder}.
 *
 * @param <S> The type of {@link CommandSource} from which the command was executed.
 */
public final class HelpQuery<S extends CommandSource> {

    private final int maxDepth, limit;
    private final @NotNull List<HelpFilter<S>> filters;
    private final @NotNull Predicate<CommandPathway<S>> rootUsagePredicate;

    /**
     * Constructs a new HelpQuery. This constructor is private to enforce
     * the use of the {@link Builder} class.
     *
     * @param maxDepth The maximum depth to traverse the command tree.
     * @param limit The maximum number of help entries to return.
     * @param filters A list of filters to apply to the help entries.
     */
    private HelpQuery(
            int maxDepth,
            int limit,
            @NotNull List<HelpFilter<S>> filters,
            @NotNull Predicate<CommandPathway<S>> rootUsagePredicate
    ) {
        this.maxDepth = maxDepth;
        this.limit = limit;
        this.filters = filters;
        this.rootUsagePredicate = rootUsagePredicate;
    }

    /**
     * Creates a new builder for constructing a {@link HelpQuery}.
     *
     * @param <S> The type of {@link CommandSource}.
     * @return A new builder instance.
     */
    public static <S extends CommandSource> Builder<S> builder() {
        return new Builder<>();
    }

    /**
     * Gets the maximum number of help entries to return.
     *
     * @return The limit.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the maximum depth to traverse the command tree when searching for help entries.
     *
     * @return The maximum depth.
     */
    public int getMaxDepth() {
        return maxDepth;
    }

    /**
     * Gets the filters to apply to the help entries.
     *
     * @return An immutable list of filters.
     */
    public @NotNull List<HelpFilter<S>> getFilters() {
        return filters;
    }

    /**
     * @return Fetches the condition for including the root's {@link CommandPathway}
     */
    public @NotNull Predicate<CommandPathway<S>> getRootUsagePredicate() {
        return rootUsagePredicate;
    }

    public @NotNull HelpQuery<S> withFilter(@NotNull HelpFilter<S> filter) {
        List<HelpFilter<S>> allFilters = new ArrayList<>(filters.size() + 1);
        allFilters.addAll(filters);
        allFilters.add(filter);
        return new HelpQuery<>(maxDepth, limit, List.copyOf(allFilters), rootUsagePredicate);
    }

    /**
     * A builder class for creating instances of {@link HelpQuery}.
     * <p>
     * This class uses a fluent API to allow for easy and readable configuration
     * of a help query with optional parameters.
     *
     * @param <S> The type of {@link CommandSource}.
     */
    public static class Builder<S extends CommandSource> {

        private final @NotNull List<HelpFilter<S>> filters = new ArrayList<>();
        private int maxDepth = 25;
        private int limit = 50;
        private @NotNull Predicate<CommandPathway<S>> rootUsagePredicate = (u) -> u.size() > 0;

        /**
         * Private constructor to enforce builder pattern.
         */
        Builder() {
        }

        /**
         * Sets the maximum depth to traverse the command tree.
         *
         * @param depth The maximum depth.
         * @return This builder instance.
         */
        public Builder<S> maxDepth(int depth) {
            this.maxDepth = depth;
            return this;
        }

        /**
         * Sets the maximum number of help entries to return.
         *
         * @param limit The limit.
         * @return This builder instance.
         */
        public Builder<S> limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the condition for including the root's <strong>MAIN</strong>command usage.
         * @param rootUsagePredicate the condition/predicate to include the root.
         * @return This builder instance.
         */
        public Builder<S> conditionalRootUsage(Predicate<CommandPathway<S>> rootUsagePredicate) {
            this.rootUsagePredicate = rootUsagePredicate;
            return this;
        }

        /**
         * Adds a filter to the query.
         *
         * @param filter The filter to add.
         * @return This builder instance.
         */
        public Builder<S> filter(@NotNull HelpFilter<S> filter) {
            filters.add(filter);
            return this;
        }

        /**
         * Builds an immutable {@link HelpQuery} instance with the configured parameters.
         *
         * @return A new {@code HelpQuery}.
         */
        public @NotNull HelpQuery<S> build() {
            return new HelpQuery<>(maxDepth, limit, List.copyOf(filters), rootUsagePredicate);
        }

    }

}
