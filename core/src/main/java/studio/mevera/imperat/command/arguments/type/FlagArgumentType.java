package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.Collections;

public class FlagArgumentType<S extends CommandSource> extends ArgumentType<S, ParsedFlagArgument<S>> {
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
    public ParsedFlagArgument<S> parse(@NotNull CommandContext<S> context, @NotNull String input) throws CommandException {
        throw new UnsupportedOperationException("FlagArgumentType does not support parse(ExecutionContext, String)");
    }

    @Override
    public ParsedFlagArgument<S> parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException {
        var currentParameter = cursor.currentParameterIfPresent();
        if (currentParameter == null) {
            throw new IllegalArgumentException("No parameter at cursor position for flag parsing");
        }
        if (!currentParameter.isFlag()) {
            throw new IllegalArgumentException("Parameter is not a flag for FlagArgumentType");
        }
        FlagArgument<S> flagArgument = currentParameter.asFlagParameter();
        String rawInput = cursor.currentRaw().orElse(null);
        if (flagArgument.isSwitch()) {
            return ParsedFlagArgument.forSwitch(flagArgument, rawInput, cursor.currentRawPosition());
        }
        // Value flag: must parse the value argument
        ArgumentType<S, ?> inputType = flagArgument.flagData().inputType();
        if (inputType == null) {
            throw new IllegalArgumentException("FlagArgumentType: value flag missing input type");
        }
        int currentPosition = cursor.currentRawPosition();
        String valueRaw = cursor.hasNextRaw() ? cursor.peekRawIfPresent() : null;
        if (valueRaw == null) {
            throw new IllegalArgumentException("No value provided for flag argument");
        }
        cursor.skipRaw(); // advance to value
        Object value = inputType.parse(context, cursor);
        return ParsedFlagArgument.forFlag(flagArgument, rawInput, valueRaw, currentPosition, currentPosition + 1, value);
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
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return flagData.isSwitch() ? 1 : 2;
    }
}
