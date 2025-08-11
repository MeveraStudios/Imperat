package studio.mevera.imperat.command.tree.help;


import studio.mevera.imperat.context.Source;

import java.util.*;

public final class HelpQuery<S extends Source> {
    
    private final int maxDepth, limit;
    private final Queue<HelpFilter<S>> filters;
    private HelpQuery(int maxDepth, int limit, Queue<HelpFilter<S>> filters) {
        this.maxDepth = maxDepth;
        this.limit = limit;
        this.filters = filters;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public int getMaxDepth() {
        return maxDepth;
    }
    
    public Queue<HelpFilter<S>> getFilters() {
        return filters;
    }
    
    public static class Builder<S extends Source> {
        
        private int maxDepth = 5;
        private int limit = 50;
        private final Queue<HelpFilter<S>> filters = new LinkedList<>();
        
        Builder() {
        }
        
        public Builder<S> maxDepth(int depth) {
            this.maxDepth = depth;
            return this;
        }
        
        public Builder<S> limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        public Builder<S> filter(HelpFilter<S> filter) {
            filters.add(filter);
            return this;
        }
        
        public HelpQuery<S> build() {
            return new HelpQuery<>(maxDepth, limit, filters);
        }
    }
    
}
