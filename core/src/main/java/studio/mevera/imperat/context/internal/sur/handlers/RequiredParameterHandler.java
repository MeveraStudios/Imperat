package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.ImperatException;

public final class RequiredParameterHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        
        if (currentParameter == null || currentRaw == null || currentParameter.isOptional()) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            var value = currentParameter.type().resolve(context, stream, stream.readInput());
            
            if (value instanceof ExtractedInputFlag extractedInputFlag) {
                context.resolveFlag(extractedInputFlag);
                stream.skip();
            } else {
                context.resolveArgument(context.getLastUsedCommand(), currentRaw, stream.currentParameterPosition(), currentParameter, value);
                stream.skip();
            }
            
            return HandleResult.NEXT_ITERATION;
        } catch (ImperatException e) {
            return HandleResult.failure(e);
        }
    }
}