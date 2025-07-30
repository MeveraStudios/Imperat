package dev.velix.imperat.context.internal.sur.handlers;

import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.context.ResolvedContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.context.internal.ExtractedInputFlag;
import dev.velix.imperat.context.internal.sur.HandleResult;
import dev.velix.imperat.exception.ImperatException;
import org.jetbrains.annotations.NotNull;

public final class RequiredParameterHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ResolvedContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameterFast();
        String currentRaw = stream.currentRawFast();
        
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