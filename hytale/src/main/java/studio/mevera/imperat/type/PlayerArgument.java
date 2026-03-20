package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.HytaleCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.DefaultValueProvider;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.util.PlayerUtil;

import java.util.List;

public class PlayerArgument extends HytaleArgumentType<PlayerRef> {

    private final PlayerSuggestionProvider SUGGESTION_RESOLVER = new PlayerSuggestionProvider();
    private final DefaultValueProvider DEFAULT_VALUE_SUPPLIER = DefaultValueProvider.of("~");

    public PlayerArgument() {
        super(PlayerRef.class, ArgTypes.PLAYER_REF, UnknownPlayerException::new);
    }

    @Override
    public @NotNull PlayerRef parse(@NotNull CommandContext<HytaleCommandSource> context, @NotNull String input) throws CommandException {
        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(input);
            }
            return context.source().asPlayerRef();
        }
        PlayerRef player = PlayerUtil.getPlayerRefByName(input);
        if (player != null) {
            return player;
        }
        throw new UnknownPlayerException(input);
    }

    @Override
    public PlayerRef parse(@NotNull ExecutionContext<HytaleCommandSource> context, @NotNull Cursor<HytaleCommandSource> cursor)
            throws CommandException {
        String input = cursor.currentRawIfPresent();
        if (input == null) {
            throw new UnknownPlayerException("<no input>");
        }
        return parse(context, input);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<HytaleCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link DefaultValueProvider} providing the default value, or empty if none.
     */
    @Override
    public DefaultValueProvider getDefaultValueProvider() {
        return DEFAULT_VALUE_SUPPLIER;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<HytaleCommandSource> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<HytaleCommandSource> context, Argument<HytaleCommandSource> argument) {
            return Universe.get().getPlayers().stream().map(PlayerRef::getUsername).toList();
        }
    }
}

