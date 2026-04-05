package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import studio.mevera.imperat.BungeeCommandSource;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.exception.ArgumentParseException;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BungeeResponseKey;

import java.util.List;

public final class ProxiedPlayerArgument extends ArgumentType<BungeeCommandSource, ProxiedPlayer> {

    private final ProxiedPlayerSuggestionProvider PROXIED_PLAYER_SUGGESTION_RESOLVER = new ProxiedPlayerSuggestionProvider();

    public ProxiedPlayerArgument() {
        super();
    }

    @Override
    public @NotNull ProxiedPlayer parse(
            @NotNull CommandContext<BungeeCommandSource> context,
            @NonNull Argument<BungeeCommandSource> argument, @NotNull String correspondingInput
    ) throws CommandException {

        if (correspondingInput.equalsIgnoreCase("me")) {
            if (context.source().isConsole()) {
                throw new ArgumentParseException(BungeeResponseKey.UNKNOWN_PLAYER, correspondingInput);
            }
            return context.source().asPlayer();
        }

        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(correspondingInput);
        if (proxiedPlayer == null) {
            throw new ArgumentParseException(BungeeResponseKey.UNKNOWN_PLAYER, correspondingInput);
        }
        return proxiedPlayer;
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<BungeeCommandSource> getSuggestionProvider() {
        return PROXIED_PLAYER_SUGGESTION_RESOLVER;
    }

    private final static class ProxiedPlayerSuggestionProvider implements SuggestionProvider<BungeeCommandSource> {

        /**
         * @param context   the context for suggestions
         * @param argument the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<BungeeCommandSource> context, Argument<BungeeCommandSource> argument) {
            return ProxyServer.getInstance().getPlayers().stream()
                           .map(ProxiedPlayer::getName)
                           .toList();
        }
    }

}
