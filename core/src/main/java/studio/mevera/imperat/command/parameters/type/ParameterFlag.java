package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.FlagParameter;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedInputFlag;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.util.Patterns;

public class ParameterFlag<S extends Source> extends BaseParameterType<S, ExtractedInputFlag> {

    protected ParameterFlag(FlagData<S> flagData) {
        super();
        suggestions.add("-" + flagData.name());
        for(var alias : flagData.aliases())
            suggestions.add("-" + alias);
    }

    public ExtractedInputFlag resolveFreeFlag(
        ExecutionContext<S> context,
        @NotNull CommandInputStream<S> commandInputStream,
        FlagData<S> freeFlag
    ) throws ImperatException {
        String rawFlag = commandInputStream.currentRaw().orElse(null);
        if (rawFlag == null) {
            throw new IllegalArgumentException();
        }
        String rawInput = null;
        Object input = null;

        if (!freeFlag.isSwitch()) {
            ParameterType<S, ?> inputType = freeFlag.inputType();
            rawInput = commandInputStream.popRaw().orElse(null);
            if (rawInput != null) {
                input = inputType.resolve(context, commandInputStream, commandInputStream.readInput());
            }
        } else {
            input = true;
        }
        return new ExtractedInputFlag(freeFlag, rawFlag, rawInput, input);
    }

    @Override
    public @Nullable ExtractedInputFlag resolve(@NotNull ExecutionContext<S> context, @NotNull CommandInputStream<S> commandInputStream, @NotNull String rawFlag) throws ImperatException {
        var currentParameter = commandInputStream.currentParameterFast();
        if (currentParameter == null)
            return null;

        if (!currentParameter.isFlag()) {
            throw new IllegalArgumentException();
        }

        FlagParameter<S> flagParameter = currentParameter.asFlagParameter();

        String rawInput = null;
        Object objInput;

        if (!flagParameter.isSwitch()) {
            ParameterType<S, ?> inputType = flagParameter.flagData().inputType();
            rawInput = commandInputStream.popRaw().orElse(null);
            if (rawInput != null) {
                assert inputType != null;
                objInput = inputType.resolve(context, commandInputStream, rawInput);
                if(objInput == null && !flagParameter.getDefaultValueSupplier().isEmpty()) {
                    String defValue = flagParameter.getDefaultValueSupplier().supply(context.source(), flagParameter);
                    if(defValue != null) {
                        objInput = inputType.resolve(context, commandInputStream, defValue);
                    }
                }
            }else {
                //"Please enter the value for flag '%s'"
                throw new MissingFlagInputException(flagParameter, rawFlag);
            }
        } else {
            objInput = true;
        }
        return new ExtractedInputFlag(flagParameter.flagData(), rawFlag, rawInput, objInput);
    }
    
    @Override
    public boolean matchesInput(String input, CommandParameter<S> parameter) {
        if(!parameter.isFlag()) {
            throw new IllegalArgumentException(String.format("Parameter '%s' isn't a flag while having parameter type of '%s'", parameter.format(),
             "ParameterFlag"));
        }

        int subStringIndex;
        if (Patterns.SINGLE_FLAG.matcher(input).matches())
            subStringIndex = 1;
        else if (Patterns.DOUBLE_FLAG.matcher(input).matches())
            subStringIndex = 2;
        else
            subStringIndex = 0;

        String flagInput = input.substring(subStringIndex);

        return parameter.asFlagParameter().flagData()
            .acceptsInput(flagInput);
    }

}
