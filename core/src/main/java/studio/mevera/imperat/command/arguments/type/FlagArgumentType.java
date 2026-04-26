package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.FlagArgument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.FlagData;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.Collections;

/**
 * The argument-type marker for flag parameters. Flag parsing itself happens
 * inside the command tree (see {@code Node#parseFlagArgument} and
 * {@code SuperCommandTree#consumeRemainingFlags}); this type exists only to
 * declare that a parameter is a flag and to drive flag-name suggestions, so
 * its {@link #parse} method should never be reached on the canonical execution
 * path.
 */
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
    public ParsedFlagArgument<S> parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        throw new UnsupportedOperationException(
                "FlagArgumentType.parse must not be called: the command tree binds flags directly via Node#parseFlagArgument."
        );
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
