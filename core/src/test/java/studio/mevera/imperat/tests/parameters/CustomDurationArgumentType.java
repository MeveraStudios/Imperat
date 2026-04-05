package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;

public class CustomDurationArgumentType<S extends CommandSource> extends ArgumentType<S, CustomDuration> {

    private final SuggestionProvider<S> resolver = SuggestionProvider.staticSuggestions(
            "permanent", "30d", "1y", "5y"
    );

    public CustomDurationArgumentType() {
        super();
    }

    @Override
    public CustomDuration parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        final long ms = TimeUtil.convertDurationToMs(input);
        if (ms == 0) {
            throw new CommandException("Bad duration input '" + input + "'");
        }
        return new CustomDuration(ms);
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return resolver;
    }

}