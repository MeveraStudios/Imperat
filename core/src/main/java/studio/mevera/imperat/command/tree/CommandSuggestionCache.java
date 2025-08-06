package studio.mevera.imperat.command.tree;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class CommandSuggestionCache<S extends Source> {
    
    private final static int ENTRY_EXPIRATION_DURATION = 10; //in seconds
    
    private final Cache<InputKey<S>, Set<ParameterNode<S, ?>>> lastNodePerInput = Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_EXPIRATION_DURATION, TimeUnit.SECONDS)
            .build();
    
    public void computeInput(S source, ArgumentInput input, ParameterNode<S, ?> lastNode) {
        InputKey<S> inputKey = new InputKey<>(source, input);
        lastNodePerInput.asMap().compute(inputKey, (k, oldSet)-> {
            if(oldSet == null) {
                Set<ParameterNode<S,?>> newLastNodes = new HashSet<>(3);
                newLastNodes.add(lastNode);
                return newLastNodes;
            }
            oldSet.add(lastNode);
            return oldSet;
        });
    }
    
    public boolean hasCache(S source, ArgumentInput input) {
        return lastNodePerInput.getIfPresent(new InputKey<>(source, input)) != null;
    }
    
    public @Nullable Set<ParameterNode<S, ?>> getLastNodes(S source, ArgumentInput input) {
        return lastNodePerInput.getIfPresent(new InputKey<>(source, input));
    }
    
    record InputKey<S extends Source>(S source, ArgumentInput input) {
    
    }
}
