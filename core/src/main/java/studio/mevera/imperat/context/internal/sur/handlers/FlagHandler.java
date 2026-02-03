package studio.mevera.imperat.context.internal.sur.handlers;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedFlagArgument;
import studio.mevera.imperat.context.internal.sur.HandleResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.exception.ShortHandFlagException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;

import java.util.Objects;
import java.util.Set;

public final class FlagHandler<S extends Source> implements ParameterHandler<S> {
    
    @Override
    public @NotNull HandleResult handle(ExecutionContext<S> context, CommandInputStream<S> stream) throws CommandException {
        CommandParameter<S> currentParameter = stream.currentParameterIfPresent();
        String currentRaw = stream.currentRawIfPresent();
        if (currentRaw == null || !Patterns.isInputFlag(currentRaw)) {
            return HandleResult.NEXT_HANDLER;
        }
        
        try {
            CommandUsage<S> usage = context.getDetectedUsage();
            
            Set<FlagParameter<S>> extracted = usage.getFlagExtractor()
                .extract(Patterns.withoutFlagSign(currentRaw));
            
            long numberOfSwitches = extracted.stream().filter(FlagParameter::isSwitch).count();
            long numberOfTrueFlags = extracted.size() - numberOfSwitches;
            
            if (extracted.size() != numberOfSwitches && extracted.size() != numberOfTrueFlags) {
                return HandleResult.failure(new ShortHandFlagException("Unsupported use of a mixture of switches and true flags!"));
            }
            
            if (extracted.size() == numberOfTrueFlags && !TypeUtility.areTrueFlagsOfSameInputType(extracted)) {
                return HandleResult.failure(new ShortHandFlagException("You cannot use compressed true-flags, while they are not of same input type"));
            }
            
            boolean areAllSwitches = extracted.size() == numberOfSwitches;
            boolean areAllTrueFlags = extracted.size() == numberOfTrueFlags;
            
            String inputRaw = areAllSwitches ? currentRaw : stream.peekRawIfPresent();
            if(inputRaw == null) {
                assert currentParameter != null;
                return HandleResult.failure(new MissingFlagInputException(currentParameter.asFlagParameter(), currentRaw));
            }
            
            if(extracted.size() == 1 && extracted.contains(Objects.requireNonNull(currentParameter).asFlagParameter())) {
                
                //resolve directly
                context.resolveFlag((ExtractedFlagArgument) currentParameter.asFlagParameter().type().resolve(context, stream, currentRaw));
                stream.skip();
                return HandleResult.NEXT_ITERATION;
            }
            else if(extracted.size() == 1) {
                resolveFlagDefaultValue(stream, currentParameter.asFlagParameter(), context);
                stream.skipParameter();
                return HandleResult.NEXT_ITERATION;
            }
            
            for(FlagParameter<S> extractedFlagParam : extracted) {
                FlagData<S> extractedFlagData = extractedFlagParam.flagData();
                context.resolveFlag(
                        new ExtractedFlagArgument(
                                extractedFlagData,
                                currentRaw,
                                inputRaw,
                                extractedFlagData.isSwitch() ? true : Objects.requireNonNull(extractedFlagData.inputType()).resolve(context, stream, inputRaw)
                        )
                );
                stream.exemptParameter(extractedFlagParam);
            }
            
            stream.skip();
            return HandleResult.NEXT_ITERATION;
        } catch (CommandException e) {
            return HandleResult.failure(e);
        }
    }

    
    private void resolveFlagDefaultValue(CommandInputStream<S> stream, FlagParameter<S> flagParameter, ExecutionContext<S> context) throws
            CommandException {
        FlagData<S> flagDataFromRaw = flagParameter.flagData();

        if (flagDataFromRaw.isSwitch()) {
            context.resolveFlag(new ExtractedFlagArgument(flagDataFromRaw, null, "false", false));
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
            context.resolveFlag(new ExtractedFlagArgument(flagDataFromRaw, null, defValue, flagValueResolved));
        }
    }
}