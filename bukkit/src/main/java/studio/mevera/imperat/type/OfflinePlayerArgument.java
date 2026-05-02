package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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

import java.util.Arrays;
import java.util.List;

/**
 * Server-side resolver for {@link OfflinePlayer} parameters. Generic
 * over the canonical source type {@code S} so custom-source plugins can
 * register cleanly without raw casts.
 */
public class OfflinePlayerArgument<S extends BukkitCommandSource> extends SimpleArgumentType<S, OfflinePlayer> {


    private final PlayerSuggestionProvider<S> playerSuggestionResolver = new PlayerSuggestionProvider<>();

    public OfflinePlayerArgument() {
        super();
    }

    @Override
    public OfflinePlayer parse(@NotNull CommandContext<S> context, @NonNull Argument<S> argument,
            @NotNull String input) throws CommandException {
        if (input.length() > 16) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_OFFLINE_PLAYER, input);
        }
        return Bukkit.getOfflinePlayer(input);
    }

    @Override
    public SuggestionProvider<S> getSuggestionProvider() {
        return playerSuggestionResolver;
    }

    private final static class PlayerSuggestionProvider<S extends BukkitCommandSource> implements SuggestionProvider<S> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<S> context, Argument<S> argument) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                           .map(OfflinePlayer::getName)
                           .toList();
        }
    }
}
