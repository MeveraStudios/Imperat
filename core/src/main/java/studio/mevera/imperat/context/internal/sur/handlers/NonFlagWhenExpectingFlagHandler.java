package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.UnknownFlagException;
import studio.mevera.imperat.util.Patterns;

import java.util.Objects;

public final class NonFlagWhenExpectingFlagHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        
        if (currentParameter == null || currentRaw == null || !currentParameter.isFlag() || Patterns.isInputFlag(currentRaw)) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            var nextParam = stream.peekParameter().orElse(null);
            
            if (nextParam == null) {
                return HandleResult.failure(new UnknownFlagException(currentRaw, context));
            } else if (!context.hasResolvedFlag(currentParameter)) {
                resolveFlagDefaultValue(stream, currentParameter.asFlagParameter(), context);
            }
            
            stream.skipParameter();
            return HandleResult.NEXT_ITERATION;
        } catch (Exception e) {
            return HandleResult.failure(new ImperatException("Error handling non-flag input when expecting flag", e, context));
        }
    }
    
    private void resolveFlagDefaultValue(CommandInputStream<S> stream, FlagParameter<S> flagParameter, ExecutionContext<S> context) throws ImperatException {
        FlagData<S> flagDataFromRaw = flagParameter.flagData();

        if (flagDataFromRaw.isSwitch()) {
            context.resolveFlag(new ExtractedInputFlag(flagDataFromRaw, null, "false", false));
            return;
        }

        String defValue = flagParameter.getDefaultValueSupplier().supply(context, flagParameter);
        if (defValue != null) {
            Object flagValueResolved = flagParameter.getDefaultValueSupplier().isEmpty() ? null :
                    Objects.requireNonNull(flagDataFromRaw.inputType()).resolve(
                            context,
                            CommandInputStream.subStream(stream, defValue),
                            defValue
                    );
            context.resolveFlag(new ExtractedInputFlag(flagDataFromRaw, null, defValue, flagValueResolved));
        }
    }
}