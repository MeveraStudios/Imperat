package studio.mevera.imperat.command.tree;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class SuggestionCache<S extends Source> {
    
    private final static int ENTRY_EXPIRATION_DURATION = 10; //in seconds
    
    private final Cache<InputKey<S>, Set<ParameterNode<S, ?>>> lastNodePerInput = Caffeine.newBuilder()
            .expireAfterWrite(ENTRY_EXPIRATION_DURATION, TimeUnit.SECONDS)
            .build();
    
    public void computeInput(Command<S> command, ArgumentInput input, ParameterNode<S, ?> lastNode) {
        InputKey<S> inputKey = new InputKey<>(command, input);
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
    
    public boolean hasCache(Command<S> command, ArgumentInput input) {
        return lastNodePerInput.getIfPresent(new InputKey<>(command, input)) != null;
    }
    
    public @Nullable Set<ParameterNode<S, ?>> getLastNodes(Command<S> command, ArgumentInput input) {
        return lastNodePerInput.getIfPresent(new InputKey<>(command, input));
    }
    
    record InputKey<S extends Source>(Command<S> root, ArgumentInput input) {
    
    }
}
