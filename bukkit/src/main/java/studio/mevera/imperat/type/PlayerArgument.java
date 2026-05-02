package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;

import java.util.List;

/**
 * Server-side resolver for {@link Player} parameters. Generic over the
 * canonical source type {@code S} so custom-source plugins can register
 * the type cleanly against their own {@code S} without raw casts —
 * {@code S} is bounded by {@link BukkitCommandSource} so all the
 * platform calls (e.g. {@code asPlayer()}) are reachable directly.
 */
public class PlayerArgument<S extends BukkitCommandSource> extends SimpleArgumentType<S, Player> {

    private final PlayerSuggestionProvider<S> SUGGESTION_RESOLVER = new PlayerSuggestionProvider<>();

    public PlayerArgument() {
        super();
    }

    @Override
    public Player parse(@NotNull CommandContext<S> context, @NonNull Argument<S> argument, @NotNull String input)
            throws CommandException {
        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw ResponseException.of(BukkitResponseKey.ONLY_PLAYER);
            }
            return context.source().asPlayer();
        }
        Player player = Bukkit.getPlayer(input);
        if (player == null) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_PLAYER, input);
        }
        return player;
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private final static class PlayerSuggestionProvider<S extends BukkitCommandSource> implements SuggestionProvider<S> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<S> context, Argument<S> argument) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
    }
}
