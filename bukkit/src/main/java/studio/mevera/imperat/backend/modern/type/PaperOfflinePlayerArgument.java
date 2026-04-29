package studio.mevera.imperat.backend.modern.type;

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

public class PaperOfflinePlayerArgument extends SimpleArgumentType<BukkitCommandSource, OfflinePlayer> {

    private final OfflinePlayerSuggestionProvider SUGGESTION_RESOLVER = new OfflinePlayerSuggestionProvider();

    public PaperOfflinePlayerArgument() {
        super();
    }

    @Override
    public OfflinePlayer parse(@NotNull CommandContext<BukkitCommandSource> context,
            @NonNull Argument<BukkitCommandSource> argument,
                               @NotNull String input) throws CommandException {
        if (input.length() > 16) {
            throw new ArgumentParseException(BukkitResponseKey.UNKNOWN_OFFLINE_PLAYER, input);
        }
        return Bukkit.getOfflinePlayer(input);
    }

    @Override
    public SuggestionProvider<BukkitCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private static final class OfflinePlayerSuggestionProvider implements SuggestionProvider<BukkitCommandSource> {
        @Override
        public List<String> provide(SuggestionContext<BukkitCommandSource> context, Argument<BukkitCommandSource> argument) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                           .map(OfflinePlayer::getName)
                           .toList();
        }
    }
}
