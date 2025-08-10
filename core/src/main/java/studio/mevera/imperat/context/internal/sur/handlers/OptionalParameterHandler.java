package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.ImperatException;

public final class OptionalParameterHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        
        if (currentParameter == null || currentRaw == null || !currentParameter.isOptional()) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            if (currentParameter.isFlag()) {
                ExtractedInputFlag value = (ExtractedInputFlag) currentParameter.type().resolve(context, stream, stream.readInput());
                context.resolveFlag(value);
                if(!currentParameter.asFlagParameter().isSwitch()) {
                    stream.skipRaw();
                }
                stream.skip();
            }else {
                resolveOptional(currentRaw, currentParameter, context, stream);
            }
            return HandleResult.NEXT_ITERATION;
        } catch (ImperatException e) {
            return HandleResult.failure(e);
        }
    }
    
    private void resolveOptional(
            String currentRaw,
            CommandParameter<S> currentParameter,
            ExecutionContext<S> context,
            CommandInputStream<S> stream
    ) throws ImperatException {
        
        int distanceFromNextOptional = calculateDistanceFromNextOptional(context, currentParameter);
        if(distanceFromNextOptional >= 2 && (stream.parametersLength()-stream.rawsLength()) >= 2) {
            //there's middle required args
            //shift parameter only, we resolve current as its default or null
            context.resolveArgument(stream, getDefaultValue(context, stream, currentParameter));
            stream.skipParameter();
            return;
        }
        
        if(     distanceFromNextOptional == 1 && // means there is next optional argument, while current is optional too, lets check if there's matching with input for smart switching of values
                context.imperatConfig().handleExecutionMiddleOptionalSkipping() &&
                !currentParameter.type().matchesInput(currentRaw, currentParameter)
        ) {
            //if it doesn't match the input while having a next optional arg, let's resolve the current for its default value,
            // then go after the next optional arg WHILE maintaining the same index/cursor on the raw input.
            context.resolveArgument(stream, getDefaultValue(context, stream, currentParameter));
            stream.skipParameter();
            return;
        }
        var value = currentParameter.type().resolve(context, stream, stream.readInput());
        context.resolveArgument(stream, value);
        stream.skip();
    }
    
    private int calculateDistanceFromNextOptional(ExecutionContext<S> context, CommandParameter<S> curr) {
        var usage = context.getDetectedUsage();
        for(int i = curr.position()+1; i < usage.getParameters().size(); i++) {
            var other = usage.getParameter(i);
            if(other != null && other.isOptional()) {
                return i-curr.position();
            }
        }
        
        return -1;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ExecutionContext<S> context, CommandInputStream<S> stream, CommandParameter<S> parameter) throws ImperatException {
        OptionalValueSupplier optionalSupplier = parameter.getDefaultValueSupplier();
        if (optionalSupplier.isEmpty()) {
            return null;
        }
        String value = optionalSupplier.supply(context.source(), parameter);

        if (value != null) {
            return (T) parameter.type().resolve(context, stream, value);
        }

        return null;
    }
}