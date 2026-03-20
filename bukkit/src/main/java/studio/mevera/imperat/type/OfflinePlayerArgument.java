package studio.mevera.imperat.type;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BukkitCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BukkitResponseKey;

import java.util.Arrays;
import java.util.List;

public class OfflinePlayerArgument extends ArgumentType<BukkitCommandSource, OfflinePlayer> {


    private final PlayerSuggestionProvider playerSuggestionResolver = new PlayerSuggestionProvider();

    public OfflinePlayerArgument() {
        super();
    }

    @Override
    public OfflinePlayer parse(@NotNull CommandContext<BukkitCommandSource> context, @NotNull String input) throws CommandException {
        if (input.length() > 16) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_OFFLINE_PLAYER, input);
        }
        return Bukkit.getOfflinePlayer(input);
    }

    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return playerSuggestionResolver;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<BukkitCommandSource> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                           .map(OfflinePlayer::getName)
                           .toList();
        }
    }
}
