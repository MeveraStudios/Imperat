package studio.mevera.imperat.type;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.HytaleSource;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.OptionalValueSupplier;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import java.util.List;

public class ParameterPlayer extends BaseParameterType<HytaleSource, PlayerRef> {

    private final PlayerSuggestionResolver SUGGESTION_RESOLVER = new PlayerSuggestionResolver();
    private final OptionalValueSupplier DEFAULT_VALUE_SUPPLIER = OptionalValueSupplier.of("~");

    public ParameterPlayer() {
        super();
    }

    private static @Nullable PlayerRef getPlayerByName(String in) {
        return Universe.get().getPlayer(in, NameMatching.DEFAULT);
    }

    @Override
    public @Nullable PlayerRef resolve(
            @NotNull ExecutionContext<HytaleSource> context,
            @NotNull CommandInputStream<HytaleSource> commandInputStream,
            @NotNull String input) throws ImperatException {

        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(input, context);
            }
            return context.source().asPlayerRef();
        }

        PlayerRef player = getPlayerByName(input);
        if (player != null) return player;

        throw new UnknownPlayerException(input, context);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionResolver<HytaleSource> getSuggestionResolver() {
        return SUGGESTION_RESOLVER;
    }

    private final static class PlayerSuggestionResolver implements SuggestionResolver<HytaleSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(SuggestionContext<HytaleSource> context, CommandParameter<HytaleSource> parameter) {
            return Universe.get().getPlayers().stream().map(PlayerRef::getUsername).toList();
        }
    }

    /**
     * Returns the default value supplier for the given source and command parameter.
     * By default, this returns an empty supplier, indicating no default value.
     *
     * @return an {@link OptionalValueSupplier} providing the default value, or empty if none.
     */
    @Override
    public OptionalValueSupplier supplyDefaultValue() {
        return DEFAULT_VALUE_SUPPLIER;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<HytaleSource> context, CommandParameter<HytaleSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return getPlayerByName(input) != null;
    }
}

