package dev.velix.imperat.context.internal.sur;

import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.context.internal.sur.handlers.ParameterHandler;
import dev.velix.imperat.exception.ImperatException;

import java.util.List;

public class ParameterChain<S extends Source> {
    private final List<ParameterHandler<S>> handlers;
    
    public ParameterChain(List<ParameterHandler<S>> handlers) {
        this.handlers = List.copyOf(handlers);
    }
    
    public void execute(ExecutionContext<S> context, CommandInputStream<S> stream) throws ImperatException {
        
        pipeLine:
        while (stream.hasNextParameter()) {
            
            for (ParameterHandler<S> handler : handlers) {
                
                // ADD: Time each individual handler
                HandleResult result = handler.handle(context, stream);
                
                switch (result) {
                    case TERMINATE:
                        break pipeLine;
                    case NEXT_ITERATION:
                        continue pipeLine;
                    case FAILURE:
                        assert result.getException() != null;
                        throw result.getException();
                }
            }
        }
        
    }
}