package studio.mevera.imperat.type;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.VelocitySource;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import java.util.List;

public final class ParameterPlayer extends BaseParameterType<VelocitySource, Player> {

    private final ProxyServer proxyServer;
    private final PlayerSuggestionResolver playerSuggestionResolver;

    public ParameterPlayer(ProxyServer server) {
        super();
        this.proxyServer = server;
        this.playerSuggestionResolver = new PlayerSuggestionResolver(server);
    }

    @Override
    public @NotNull Player resolve(
            @NotNull ExecutionContext<VelocitySource> context,
            @NotNull CommandInputStream<VelocitySource> commandInputStream,
            @NotNull String input) throws ImperatException {
        
        if (input.equalsIgnoreCase("me")) {
            if (context.source().isConsole()) {
                throw new UnknownPlayerException(input, context);
            }
            return context.source().asPlayer();
        }
        return proxyServer.getPlayer(input.toLowerCase()).orElseThrow(() -> new UnknownPlayerException(input, context));
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<VelocitySource> context, CommandParameter<VelocitySource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        return input.length() < 16;
    }

    /**
     * Returns the suggestion resolver associated with this parameter type.
     *
     * @return the suggestion resolver for generating suggestions based on the parameter type.
     */
    @Override
    public SuggestionResolver<VelocitySource> getSuggestionResolver() {
        return playerSuggestionResolver;
    }

    private final static class PlayerSuggestionResolver implements SuggestionResolver<VelocitySource> {

        private final ProxyServer proxyServer;

        PlayerSuggestionResolver(ProxyServer server) {
            this.proxyServer = server;
        }

        /**
         * @param context   the context for suggestions
         * @param parameter the parameter of the value to complete
         * @return the auto-completed suggestions of the current argument
         */
        @Override
        public List<String> autoComplete(SuggestionContext<VelocitySource> context, CommandParameter<VelocitySource> parameter) {
            return proxyServer.getAllPlayers().stream().map(Player::getUsername).toList();
        }
    }
}
