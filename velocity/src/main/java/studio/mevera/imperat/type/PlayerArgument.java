package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.VelocityCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.VelocityResponseKey;

import java.util.List;

public final class PlayerArgument extends ArgumentType<VelocityCommandSource, Player> {

    private final ProxyServer proxyServer;
    private final PlayerSuggestionProvider playerSuggestionResolver;

    public PlayerArgument(ProxyServer server) {
        super();
        this.proxyServer = server;
        this.playerSuggestionResolver = new PlayerSuggestionProvider(server);
    }

    @Override
    public @NotNull Player parse(@NotNull CommandContext<VelocityCommandSource> context, @NonNull Argument<VelocityCommandSource> argument,
            @NotNull String input) throws CommandException {
        if (input.equalsIgnoreCase("me")) {
            if (context.source().isConsole()) {
                throw ResponseException.of(VelocityResponseKey.ONLY_PLAYER)
                              .withPlaceholder("player", input);
            }
            return context.source().asPlayer();
        }
        return proxyServer.getPlayer(input.toLowerCase())
                       .orElseThrow(() -> ResponseException.of(VelocityResponseKey.UNKNOWN_PLAYER)
                                                  .withPlaceholder("player", input));
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<VelocityCommandSource> getSuggestionProvider() {
        return playerSuggestionResolver;
    }

    private final static class PlayerSuggestionProvider implements SuggestionProvider<VelocityCommandSource> {

        private final ProxyServer proxyServer;

        PlayerSuggestionProvider(ProxyServer server) {
            this.proxyServer = server;
        }

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<VelocityCommandSource> context, Argument<VelocityCommandSource> argument) {
            return proxyServer.getAllPlayers().stream().map(Player::getUsername).toList();
        }
    }
}
