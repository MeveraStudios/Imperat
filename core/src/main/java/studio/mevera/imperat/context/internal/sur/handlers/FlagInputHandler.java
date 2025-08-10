package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.command.parameters.type.ParameterTypes;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.exception.ShortHandFlagException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;

import java.util.Objects;
import java.util.Set;

public final class FlagInputHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        if (currentParameter == null || currentRaw == null || !currentParameter.isFlag() || !Patterns.isInputFlag(currentRaw)) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            CommandUsage<S> usage = context.getDetectedUsage();
            
            Set<FlagData<S>> extracted = usage.getFlagExtractor()
                .extract(Patterns.withoutFlagSign(currentRaw), context);
            
            long numberOfSwitches = extracted.stream().filter(FlagData::isSwitch).count();
            long numberOfTrueFlags = extracted.size() - numberOfSwitches;
            
            if (extracted.size() != numberOfSwitches && extracted.size() != numberOfTrueFlags) {
                return HandleResult.failure(new ShortHandFlagException("Unsupported use of a mixture of switches and true flags!", context));
            }
            
            if (extracted.size() == numberOfTrueFlags && !TypeUtility.areTrueFlagsOfSameInputTpe(extracted)) {
                return HandleResult.failure(new ShortHandFlagException("You cannot use compressed true-flags, while they are not of same input type", context));
            }
            
            boolean areAllSwitches = extracted.size() == numberOfSwitches;
            boolean areAllTrueFlags = extracted.size() == numberOfTrueFlags;
            
            String inputRaw = areAllSwitches ? currentRaw : stream.peekRawIfPresent();
            if(inputRaw == null) {
                throw new MissingFlagInputException(currentParameter.asFlagParameter(), currentRaw, context);
            }
            
            if(extracted.size() == 1 && extracted.contains(currentParameter.asFlagParameter().flagData())) {
                
                //resolve directly
                context.resolveFlag(ParameterTypes.flag(currentParameter.asFlagParameter().flagData()).resolve(context, stream, currentRaw));
                stream.skip();
                return HandleResult.NEXT_ITERATION;
            }
            else if(extracted.size() == 1) {
                resolveFlagDefaultValue(stream, currentParameter.asFlagParameter(), context);
                stream.skipParameter();
                return HandleResult.NEXT_ITERATION;
            }
            
            for(FlagData<S> extractedFlagData : extracted) {
                CommandParameter<S> matchingParam = getMatchingFlagParam(usage, stream, currentParameter.position()+1, extractedFlagData);
                if(matchingParam != null) {
                    context.resolveFlag(
                            new ExtractedInputFlag(
                                    extractedFlagData,
                                    currentRaw,
                                    inputRaw,
                                    extractedFlagData.isSwitch() ? true : Objects.requireNonNull(extractedFlagData.inputType()).resolve(context, stream, inputRaw)
                            )
                    );
                    stream.exemptParameter(matchingParam);
                }
            }
            
            stream.skip();
            return HandleResult.NEXT_ITERATION;
        } catch (ImperatException e) {
            return HandleResult.failure(e);
        }
    }
    
    private @Nullable CommandParameter<S> getMatchingFlagParam(
            CommandUsage<S> usage,
            CommandInputStream<S> inputStream,
            int start,
            FlagData<S> extractedFlagData
    ) {
        for (int i = start; i < inputStream.parametersLength(); i++) {
            var param = usage.getParameter(i);
            if(param == null)break;
            if(param.isFlag() && param.asFlagParameter().flagData().equals(extractedFlagData)) {
                return param;
            }
        }
        return null;
    }
    
    private void resolveFlagDefaultValue(CommandInputStream<S> stream, FlagParameter<S> flagParameter, ExecutionContext<S> context) throws ImperatException {
        FlagData<S> flagDataFromRaw = flagParameter.flagData();

        if (flagDataFromRaw.isSwitch()) {
            context.resolveFlag(new ExtractedInputFlag(flagDataFromRaw, null, "false", false));
            return;
        }

        String defValue = flagParameter.getDefaultValueSupplier().supply(context.source(), flagParameter);
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