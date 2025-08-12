package studio.mevera.imperat.command.tree.help;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.Source;

import java.util.*;

/**
 * An immutable data class representing a query for help documentation.
 * <p>
 * This class encapsulates the various parameters that can be used to filter and
 * limit help entries, such as maximum depth, a result limit, and a queue of
 * specific filters. It is designed to be constructed using its nested {@link Builder}.
 *
 * @param <S> The type of {@link Source} from which the command was executed.
 */
public final class HelpQuery<S extends Source> {
    
    private final int maxDepth, limit;
    private final @NotNull Queue<HelpFilter<S>> filters;
    
    /**
     * Constructs a new HelpQuery. This constructor is private to enforce
     * the use of the {@link Builder} class.
     *
     * @param maxDepth The maximum depth to traverse the command tree.
     * @param limit The maximum number of help entries to return.
     * @param filters A queue of filters to apply to the help entries.
     */
    private HelpQuery(int maxDepth, int limit, @NotNull Queue<HelpFilter<S>> filters) {
        this.maxDepth = maxDepth;
        this.limit = limit;
        this.filters = filters;
    }
    
    /**
     * Creates a new builder for constructing a {@link HelpQuery}.
     *
     * @param <S> The type of {@link Source}.
     * @return A new builder instance.
     */
    public static <S extends Source> Builder<S> builder() {
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
     * Gets the queue of filters to apply to the help entries.
     *
     * @return An unmodifiable queue of filters.
     */
    public @NotNull Queue<HelpFilter<S>> getFilters() {
        return filters;
    }
    
    /**
     * A builder class for creating instances of {@link HelpQuery}.
     * <p>
     * This class uses a fluent API to allow for easy and readable configuration
     * of a help query with optional parameters.
     *
     * @param <S> The type of {@link Source}.
     */
    public static class Builder<S extends Source> {
        
        private int maxDepth = 25;
        private int limit = 50;
        private final @NotNull Queue<HelpFilter<S>> filters = new LinkedList<>();
        
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
         * Adds a filter to the queue of filters.
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
            return new HelpQuery<>(maxDepth, limit, filters);
        }
    }
    
}
