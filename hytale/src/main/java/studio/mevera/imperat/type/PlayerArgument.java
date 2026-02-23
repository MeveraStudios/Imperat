package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.DefaultValueProvider;
import studio.mevera.imperat.context.Context;
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
    public @Nullable PlayerRef parse(
            @NotNull ExecutionContext<HytaleSource> context,
            @NotNull Cursor<HytaleSource> cursor,
            @NotNull String correspondingInput) throws CommandException {

        if (correspondingInput.equalsIgnoreCase("me") || correspondingInput.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(correspondingInput);
            }
            return context.source().asPlayerRef();
        }

        PlayerRef player = PlayerUtil.getPlayerRefByName(correspondingInput);
        if (player != null) {
            return player;
        }

        throw new UnknownPlayerException(correspondingInput);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<HytaleSource> getSuggestionProvider() {
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

    @Override
    public boolean matchesInput(int rawPosition, Context<HytaleSource> context, Argument<HytaleSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return PlayerUtil.getPlayerRefByName(input) != null;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<HytaleSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<HytaleSource> context, Argument<HytaleSource> parameter) {
            return Universe.get().getPlayers().stream().map(PlayerRef::getUsername).toList();
        }
    }
}

