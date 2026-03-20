package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
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
    public CustomDuration parse(@NotNull CommandContext<S> context, @NotNull String input) throws CommandException {
        final long ms = TimeUtil.convertDurationToMs(input);
        if (ms == 0) {
            throw new CommandException("Bad duration input '" + input + "'");
        }
        return new CustomDuration(ms);
    }

    @Override
    public CustomDuration parse(@NotNull ExecutionContext<S> context, @NotNull Cursor<S> cursor) throws CommandException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new CommandException("No input for duration");
        }
        return parse(context, input);
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return resolver;
    }

}