package studio.mevera.imperat.paper.type;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.paper.PaperCommandSource;
import studio.mevera.imperat.paper.PaperResponseKey;
import studio.mevera.imperat.providers.SuggestionProvider;

import java.util.List;

public class PaperPlayerArgument extends SimpleArgumentType<PaperCommandSource, Player> {

    private final PlayerSuggestionProvider SUGGESTION_RESOLVER = new PlayerSuggestionProvider();

    public PaperPlayerArgument() {
        super();
    }

    @Override
    public Player parse(@NotNull CommandContext<PaperCommandSource> context,
                        @NonNull Argument<PaperCommandSource> argument,
                        @NotNull String input) throws CommandException {
        if (input.equalsIgnoreCase("me") || input.equalsIgnoreCase("~")) {
            if (context.source().isConsole()) {
                throw ResponseException.of(PaperResponseKey.ONLY_PLAYER);
            }
            return context.source().asPlayer();
        }
        Player player = Bukkit.getPlayer(input);
        if (player == null) {
            throw new ArgumentParseException(PaperResponseKey.UNKNOWN_PLAYER, input);
        }
        return player;
    }

    @Override
    public SuggestionProvider<PaperCommandSource> getSuggestionProvider() {
        return SUGGESTION_RESOLVER;
    }

    private static final class PlayerSuggestionProvider implements SuggestionProvider<PaperCommandSource> {
        @Override
        public List<String> provide(SuggestionContext<PaperCommandSource> context, Argument<PaperCommandSource> argument) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
    }
}
