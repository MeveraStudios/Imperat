package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.types.ParameterPlayer;
import studio.mevera.imperat.util.TypeWrap;

public final class VelocityConfigBuilder extends ConfigBuilder<VelocitySource, VelocityImperat, VelocityConfigBuilder> {

    private final PluginContainer plugin;
    private final ProxyServer proxyServer;

    VelocityConfigBuilder(@NotNull PluginContainer plugin, @NotNull ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        addThrowableHandlers();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<VelocitySource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
    }

    private void registerSourceResolvers() {
        config.registerSourceResolver(CommandSource.class, VelocitySource::origin);

        config.registerSourceResolver(Player.class, (source) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, imperat, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );
    }

    private void registerValueResolvers() {
        config.registerParamType(Player.class, new ParameterPlayer(proxyServer));
    }

    @Override
    public @NotNull VelocityImperat build() {
        return new VelocityImperat(plugin, proxyServer, this.config);
    }
}