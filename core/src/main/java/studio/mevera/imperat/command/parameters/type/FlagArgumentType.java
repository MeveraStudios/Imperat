package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ExtractedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.MissingFlagInputException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.Collections;
import java.util.Set;

public class FlagArgumentType<S extends Source> extends ArgumentType<S, ExtractedFlagArgument> {

    private final FlagData<S> flagData;

    protected FlagArgumentType(FlagData<S> flagData) {
        super();
        this.flagData = flagData;
        suggestions.add("-" + flagData.name());
        for (var alias : flagData.aliases()) {
            suggestions.add("-" + alias);
        }
    }

    @Override
    public @Nullable ExtractedFlagArgument parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        var currentParameter = cursor.currentParameterIfPresent();
        if (currentParameter == null) {
            return null;
        }

        if (!currentParameter.isFlag()) {
            throw new IllegalArgumentException();
        }

        FlagArgument<S> FlagArgument = currentParameter.asFlagParameter();

        String rawInput = null;
        Object objInput;

        if (!FlagArgument.isSwitch()) {
            ArgumentType<S, ?> inputType = FlagArgument.flagData().inputType();
            rawInput = cursor.popRaw().orElse(null);
            if (rawInput != null) {
                assert inputType != null;
                objInput = inputType.parse(context, cursor, rawInput);
                if (objInput == null && !FlagArgument.getDefaultValueSupplier().isEmpty()) {
                    String defValue = FlagArgument.getDefaultValueSupplier().supply(context, FlagArgument);
                    if (defValue != null) {
                        objInput = inputType.parse(context, cursor, defValue);
                    }
                }
            } else {
                //"Please enter the value for flag '%s'"
                throw new MissingFlagInputException(Set.of(FlagArgument.name()), correspondingInput);
            }
        } else {
            objInput = true;
        }
        return new ExtractedFlagArgument(FlagArgument.flagData(), correspondingInput, rawInput, objInput);
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<S> context, Argument<S> parameter) {
        String input = context.arguments().getOr(rawPosition, null);
        if (input == null) {
            return false;
        }

        if (!parameter.isFlag()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Parameter '%s' isn't a flag while having parameter type of '%s'",
                            parameter.format(), "FlagArgumentType"
                    )
            );
        }

        FlagArgument<S> FlagArgument = parameter.asFlagParameter();
        ArgumentType<S, ?> inputType = FlagArgument.flagData().inputType();
        boolean matchesForFlagInput = true;
        int nextPos = rawPosition + 1;

        if (inputType != null && !FlagArgument.isSwitch() && nextPos < context.arguments().size()) {
            String nextInput = context.arguments().getOr(nextPos, null);
            if (nextInput == null) {
                matchesForFlagInput = false;
            } else {
                matchesForFlagInput = inputType.matchesInput(nextPos, context, parameter);
            }
        }
        return parameter.asFlagParameter().flagData()
                       .acceptsInput(input) && matchesForFlagInput;
    }

    @Override
    public SuggestionResolver<S> getSuggestionResolver() {
        return (ctx, param) -> {
            if (!param.isFlag()) {
                return Collections.emptyList();
            }

            FlagArgument<S> FlagArgument = param.asFlagParameter();
            var argToComplete = ctx.getArgToComplete();
            if (FlagArgument.isSwitch() ||
                        argToComplete.index() == 0 ||
                        !FlagArgument.flagData().acceptsInput(ctx.arguments().get(argToComplete.index() - 1))) {
                return this.suggestions;
            }
            //flag is a true flag AND the next position is its value
            var specificParamType = FlagArgument.inputSuggestionResolver();
            if (specificParamType != null) {
                return specificParamType.autoComplete(ctx, param);
            }
            ArgumentType<S, ?> flagInputValueType = ctx.imperatConfig().getArgumentType(FlagArgument.inputValueType());
            if (flagInputValueType != null) {
                return flagInputValueType.getSuggestionResolver().autoComplete(ctx, param);
            }
            return Collections.emptyList();
        };
    }

    @Override
    public int getNumberOfParametersToConsume() {
        return flagData.isSwitch() ? 1 : 2;
    }
}
