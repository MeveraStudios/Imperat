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
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;

import java.util.List;

public class PlayerArgument extends SimpleArgumentType<BukkitCommandSource, Player> {

    private final PlayerSuggestionProvider SUGGESTION_RESOLVER = new PlayerSuggestionProvider();

    public PlayerArgument() {
        super();
    }

    @Override
    public Player parse(@NotNull CommandContext<BukkitCommandSource> context, @NonNull Argument<BukkitCommandSource> argument, @NotNull String input)
            throws CommandException {
        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_PLAYER, input);
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
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<BukkitCommandSource> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
    }
}
