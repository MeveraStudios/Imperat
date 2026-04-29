package studio.mevera.imperat.paper.type;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.paper.PaperCommandSource;
import studio.mevera.imperat.paper.PaperResponseKey;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.Arrays;
import java.util.List;

public class PaperOfflinePlayerArgument extends SimpleArgumentType<PaperCommandSource, OfflinePlayer> {

    private final OfflinePlayerSuggestionProvider SUGGESTION_RESOLVER = new OfflinePlayerSuggestionProvider();

    public PaperOfflinePlayerArgument() {
        super();
    }

    @Override
    public OfflinePlayer parse(@NotNull CommandContext<PaperCommandSource> context,
                               @NonNull Argument<PaperCommandSource> argument,
                               @NotNull String input) throws CommandException {
        if (input.length() > 16) {
            throw new ArgumentParseException(PaperResponseKey.UNKNOWN_OFFLINE_PLAYER, input);
        }
        return Bukkit.getOfflinePlayer(input);
    }

    @Override
    public SuggestionProvider<PaperCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private static final class OfflinePlayerSuggestionProvider implements SuggestionProvider<PaperCommandSource> {
        @Override
        public List<String> provide(SuggestionContext<PaperCommandSource> context, Argument<PaperCommandSource> argument) {
            return Arrays.stream(Bukkit.getOfflinePlayers())
                           .map(OfflinePlayer::getName)
                           .toList();
        }
    }
}
