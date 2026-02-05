package studio.mevera.imperat.type;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.BungeeSource;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.resolvers.SuggestionResolver;

import java.util.List;

public final class ProxiedPlayerArgument extends ArgumentType<BungeeSource, ProxiedPlayer> {

    private final ProxiedPlayerSuggestionResolver PROXIED_PLAYER_SUGGESTION_RESOLVER = new ProxiedPlayerSuggestionResolver();

    public ProxiedPlayerArgument() {
        super();
    }

    @Override
    public @NotNull ProxiedPlayer resolve(
            @NotNull ExecutionContext<BungeeSource> context,
            @NotNull Cursor<BungeeSource> cursor,
            @NotNull String correspondingInput) throws CommandException {

        if (correspondingInput.equalsIgnoreCase("me")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(correspondingInput);
            }
            return context.source().asPlayer();
        }

        ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(correspondingInput);
        if (proxiedPlayer == null) {
            throw new UnknownPlayerException(correspondingInput);
        }
        return proxiedPlayer;
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<BungeeSource> context, Argument<BungeeSource> parameter) {
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
    public SuggestionResolver<BungeeSource> getSuggestionResolver() {
        return PROXIED_PLAYER_SUGGESTION_RESOLVER;
    }

    private final static class ProxiedPlayerSuggestionResolver implements SuggestionResolver<BungeeSource> {

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(SuggestionContext<BungeeSource> context, Argument<BungeeSource> parameter) {
            return ProxyServer.getInstance().getPlayers().stream()
                           .map(ProxiedPlayer::getName)
                           .toList();
        }
    }

}
