package studio.mevera.imperat.context.internal.sur;

import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.sur.handlers.ParameterHandler;
import studio.mevera.imperat.exception.ImperatException;
import java.util.List;

public class ParameterChain<S extends Source> {
    private final List<ParameterHandler<S>> handlers;
    
    public ParameterChain(List<ParameterHandler<S>> handlers) {
        this.handlers = List.copyOf(handlers);
    }
    
    public void execute(ExecutionContext<S> context, CommandInputStream<S> stream) throws ImperatException {
        
        pipeLine:
        while (stream.isCurrentParameterAvailable()) {
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