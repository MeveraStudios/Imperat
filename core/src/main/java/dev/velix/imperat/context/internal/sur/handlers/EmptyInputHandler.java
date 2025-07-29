package dev.velix.imperat.context.internal.sur.handlers;

import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.FlagParameter;
import dev.velix.imperat.command.parameters.OptionalValueSupplier;
import dev.velix.imperat.context.FlagData;
import dev.velix.imperat.context.ResolvedContext;
import dev.velix.imperat.context.Source;
import dev.velix.imperat.context.internal.CommandInputStream;

import dev.velix.imperat.context.internal.sur.HandleResult;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.InvalidSyntaxException;
import dev.velix.imperat.util.ImperatDebugger;

public final class EmptyInputHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public HandleResult handle(ResolvedContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameter().orElse(null);
        if (currentParameter == null) {
            return HandleResult.TERMINATE;
        }
        
        String currentRaw = stream.currentRaw().orElse(null);
        if (currentRaw != null) {
            return HandleResult.NEXT_HANDLER; // Not empty input, let other handlers process
        }
        
        try {
            if (currentParameter.isOptional()) {
                handleEmptyOptional(currentParameter, stream, context);
                stream.skipParameter();
            }
            else {
                //required
                throw new InvalidSyntaxException();
            }
            // Handle remaining optional parameters
            return HandleResult.NEXT_ITERATION;
        } catch (ImperatException e) {
            return HandleResult.failure(e);
        }
    }
    
    private void handleEmptyOptional(CommandParameter<S> optionalEmptyParameter, CommandInputStream<S> stream, 
                                   ResolvedContext<S> context) throws ImperatException {
        if (optionalEmptyParameter.isFlag()) {
            FlagParameter<S> flagParameter = optionalEmptyParameter.asFlagParameter();
            FlagData<S> flag = flagParameter.flagData();
            Object value = null;
            
            if (flag.isSwitch()) {
                value = false;
            } else {
                String defaultStrValue = flagParameter.getDefaultValueSupplier()
                    .supply(context.source(), flagParameter);
                if (defaultStrValue != null) {
                    value = flag.inputType().resolve(context, CommandInputStream.subStream(stream, defaultStrValue), defaultStrValue);
                }
            }
            
            context.resolveFlag(flag, null, null, value);
        } else {
            context.resolveArgument(context.getLastUsedCommand(), null, stream.position().getParameter(),
                optionalEmptyParameter, getDefaultValue(context, stream, optionalEmptyParameter));
        }
    }
    @SuppressWarnings("unchecked")
    private <T> T getDefaultValue(ResolvedContext<S> context, CommandInputStream<S> stream, CommandParameter<S> parameter) throws ImperatException {
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
