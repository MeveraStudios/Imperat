package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
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
    public @Nullable CustomDuration parse(
            @NotNull ExecutionContext<S> context,
            @NotNull Cursor<S> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        final long ms = TimeUtil.convertDurationToMs(correspondingInput);
        if (ms == 0) {
            throw new CommandException("Bad duration input '" + correspondingInput + "'");
        }
        return new CustomDuration(ms);
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return resolver;
    }

}