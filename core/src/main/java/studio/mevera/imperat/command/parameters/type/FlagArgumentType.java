package studio.mevera.imperat.command.parameters.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.FlagArgument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.ResponseKey;

import java.util.Collections;
import java.util.Set;

public class FlagArgumentType<S extends Source> extends ArgumentType<S, ParsedFlagArgument<S>> {

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
    public @Nullable ParsedFlagArgument<S> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput) throws
            CommandException {
        var currentParameter = cursor.currentParameterIfPresent();
        if (currentParameter == null) {
            return null;
        }

        if (!currentParameter.isFlag()) {
            throw new IllegalArgumentException();
        }

        FlagArgument<S> flagArgument = currentParameter.asFlagParameter();

        String rawInput;
        Object objInput;

        if (!flagArgument.isSwitch()) {
            ArgumentType<S, ?> inputType = flagArgument.flagData().inputType();
            int currentPosition = cursor.currentRawPosition();
            rawInput = cursor.popRaw().orElse(null);
            if (rawInput != null) {
                assert inputType != null;
                objInput = inputType.parse(context, cursor, rawInput);
                if (objInput == null && !flagArgument.getDefaultValueSupplier().isEmpty()) {
                    String defValue = flagArgument.getDefaultValueSupplier().provide(context, flagArgument);
                    if (defValue != null) {
                        objInput = inputType.parse(context, cursor, defValue);
                    }
                }
                return ParsedFlagArgument.forFlag(
                        flagArgument, correspondingInput, rawInput,
                        currentPosition, currentPosition+1,
                        objInput
                );

            } else {
                //"Please enter the value for flag '%s'"
                throw new CommandException(ResponseKey.MISSING_FLAG_INPUT)
                              .withPlaceholder("flags", String.join(",", Set.of(flagArgument.getName())));
            }
        } else {
            return ParsedFlagArgument.forSwitch(flagArgument, correspondingInput, cursor.currentRawPosition());
        }
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<S> context, Argument<S> parameter) {
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
    public SuggestionProvider<S> getSuggestionProvider() {
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
                return specificParamType.provide(ctx, param);
            }
            ArgumentType<S, ?> flagInputValueType = ctx.imperatConfig().getArgumentType(FlagArgument.inputValueType());
            if (flagInputValueType != null) {
                return flagInputValueType.getSuggestionProvider().provide(ctx, param);
            }
            return Collections.emptyList();
        };
    }

    @Override
    public int getNumberOfParametersToConsume() {
        return flagData.isSwitch() ? 1 : 2;
    }
}
