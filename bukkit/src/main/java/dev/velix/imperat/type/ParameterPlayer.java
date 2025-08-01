package dev.velix.imperat.type;

import dev.velix.imperat.BukkitSource;
import dev.velix.imperat.command.parameters.CommandParameter;
import dev.velix.imperat.command.parameters.OptionalValueSupplier;
import dev.velix.imperat.command.parameters.type.BaseParameterType;
import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.context.SuggestionContext;
import dev.velix.imperat.context.internal.CommandInputStream;
import dev.velix.imperat.exception.ImperatException;
import dev.velix.imperat.exception.UnknownPlayerException;
import dev.velix.imperat.resolvers.SuggestionResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ParameterPlayer extends BaseParameterType<BukkitSource, Player> {

    private final PlayerSuggestionResolver SUGGESTION_RESOLVER = new PlayerSuggestionResolver();
    private final OptionalValueSupplier DEFAULT_VALUE_SUPPLIER = OptionalValueSupplier.of("~");

    public ParameterPlayer() {
        super();
    }

    @Override
    public @Nullable Player resolve(
            @NotNull ExecutionContext<BukkitSource> context,
            @NotNull CommandInputStream<BukkitSource> commandInputStream,
            @NotNull String input) throws ImperatException {

        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(input);
            }
            return context.source().asPlayer();
        }

        final Player player = Bukkit.getPlayerExact(input);
        if (player != null) return player;

        throw new UnknownPlayerException(input);
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionResolver<BukkitSource> getSuggestionResolver() {
        return SUGGESTION_RESOLVER;
    }

    private final static class PlayerSuggestionResolver implements SuggestionResolver<BukkitSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(SuggestionContext<BukkitSource> context, CommandParameter<BukkitSource> parameter) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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
}
