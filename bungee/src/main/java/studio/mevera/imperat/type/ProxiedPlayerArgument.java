package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.responses.BungeeResponseKey;

import java.util.List;

public final class ProxiedPlayerArgument extends ArgumentType<BungeeSource, ProxiedPlayer> {

    private final ProxiedPlayerSuggestionProvider PROXIED_PLAYER_SUGGESTION_RESOLVER = new ProxiedPlayerSuggestionProvider();

    public ProxiedPlayerArgument() {
        super();
    }

    @Override
    public @NotNull ProxiedPlayer parse(
            @NotNull ExecutionContext<BungeeSource> context,
            @NotNull Cursor<BungeeSource> cursor,
            @NotNull String correspondingInput) throws CommandException {

        if (correspondingInput.equalsIgnoreCase("me")) {
            if (context.source().isConsole()) {
                throw new CommandException(BungeeResponseKey.UNKNOWN_PLAYER)
                              .withPlaceholder("input", correspondingInput);
            }
            return context.source().asPlayer();
        }

        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(correspondingInput);
        if (proxiedPlayer == null) {
            throw new CommandException(BungeeResponseKey.UNKNOWN_PLAYER)
                          .withPlaceholder("input", correspondingInput);
        }
        return proxiedPlayer;
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<BungeeSource> context, Argument<BungeeSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return ProxyServer.getInstance().getPlayer(input) != null;
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionProvider<BungeeSource> getSuggestionProvider() {
        return PROXIED_PLAYER_SUGGESTION_RESOLVER;
    }

    private final static class ProxiedPlayerSuggestionProvider implements SuggestionProvider<BungeeSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> provide(SuggestionContext<BungeeSource> context, Argument<BungeeSource> parameter) {
            return ProxyServer.getInstance().getPlayers().stream()
                           .map(ProxiedPlayer::getName)
                           .toList();
        }
    }

}
