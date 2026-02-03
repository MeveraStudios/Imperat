package studio.mevera.imperat.context.internal.sur;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedFlagArgument;
import studio.mevera.imperat.context.internal.sur.handlers.ParameterHandler;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.exception.ShortHandFlagException;
import studio.mevera.imperat.util.Patterns;
import studio.mevera.imperat.util.TypeUtility;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ParameterChain<S extends Source> {
    private final List<ParameterHandler<S>> handlers;
    
    public ParameterChain(List<ParameterHandler<S>> handlers) {
        this.handlers = List.copyOf(handlers);
    }
    
    public void execute(ExecutionContext<S> context, CommandInputStream<S> stream) throws CommandException {
        
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


        var usage = context.getDetectedUsage();
        for (int rPos = 0; rPos < stream.rawsLength(); rPos++) {
            String raw = context.getRawArgument(rPos);
            if(!Patterns.isInputFlag(raw))
                continue;
            String nextRaw = rPos + 1 < stream.rawsLength() ? context.getRawArgument(rPos + 1) : null;
            //identify if its a registered flag
            Set<FlagParameter<S>> extracted = usage.getFlagExtractor().extract(Patterns.withoutFlagSign(raw));
            String inputRaw = validateExtractedFlagsAndGetInputRaw(raw, nextRaw, extracted);

            //all flags here must be resolved inside the context
            for(var flagParam : extracted) {
                FlagData<S> extractedFlagData = flagParam.flagData();
                context.resolveFlag(
                        new ExtractedFlagArgument(
                                extractedFlagData,
                                raw,
                                inputRaw,
                                extractedFlagData.isSwitch() ? true : Objects.requireNonNull(extractedFlagData.inputType()).resolve(context,
                                        CommandInputStream.startingFrom(rPos, stream.parametersLength(), stream), inputRaw)
                        )
                );
            }

        }

        for(FlagParameter<S> registered : usage.getFlagExtractor().getRegisteredFlags()) {
            if (context.hasResolvedFlag(registered.flagData())) {
                continue;
            }
            resolveFlagDefaultValue(registered, context);
        }

    }

    private String validateExtractedFlagsAndGetInputRaw(String currentRaw, @Nullable String nextRaw, Set<FlagParameter<S>> extracted) throws CommandException{
        long numberOfSwitches = extracted.stream().filter(FlagParameter::isSwitch).count();
        long numberOfTrueFlags = extracted.size() - numberOfSwitches;

        if (extracted.size() != numberOfSwitches && extracted.size() != numberOfTrueFlags) {
            throw new ShortHandFlagException("Unsupported use of a mixture of switches and true flags!");
        }

        if (extracted.size() == numberOfTrueFlags && !TypeUtility.areTrueFlagsOfSameInputType(extracted)) {
            throw new ShortHandFlagException("You cannot use compressed true-flags, while they are not of same input type");
        }

        boolean areAllSwitches = extracted.size() == numberOfSwitches;

        String inputRaw = areAllSwitches ? currentRaw : nextRaw;
        if(!areAllSwitches && inputRaw == null) {
            throw new MissingFlagInputException(extracted.stream().map(FlagParameter::name).collect(Collectors.toSet()), currentRaw);
        }
        return inputRaw;
    }

    private void resolveFlagDefaultValue(FlagParameter<S> flagParameter, ExecutionContext<S> context) throws
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
                                                       CommandInputStream.ofSingleString(flagParameter, defValue),
                                                       defValue
                                               );
            context.resolveFlag(new ExtractedFlagArgument(flagDataFromRaw, null, defValue, flagValueResolved));
        }
    }

}