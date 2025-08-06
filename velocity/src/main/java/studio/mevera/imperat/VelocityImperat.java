package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.types.ParameterPlayer;

public final class VelocityImperat extends BaseImperat<VelocitySource> {

    final PluginContainer plugin;
    private final ProxyServer proxyServer;

    public static VelocityConfigBuilder builder(@NotNull PluginContainer plugin, @NotNull ProxyServer proxyServer) {
        return new VelocityConfigBuilder(plugin, proxyServer);
    }

    VelocityImperat(
        @NotNull PluginContainer plugin,
        @NotNull ProxyServer proxyServer,
        @NotNull ImperatConfig<VelocitySource> config
    ) {
        super(config);
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        registerDefaultResolvers();
    }

    private void registerDefaultResolvers() {
        // Register Player and other source/value resolvers
        config.registerParamType(Player.class, new ParameterPlayer(proxyServer));

        // Define custom exception handling for unknown players
        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );

        // Register source resolver for Player
        config.registerSourceResolver(Player.class, (source) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    @Override
    public void registerCommand(Command<VelocitySource> command) {
        super.registerCommand(command);
        CommandManager manager = proxyServer.getCommandManager();
        try {
            InternalVelocityCommand internalCmd = new InternalVelocityCommand(this, command, manager);
            manager.register(internalCmd.getMeta(), internalCmd);
        } catch (Exception ex) {
            config.handleExecutionThrowable(ex, null, VelocityImperat.class, "registerCommand");
        }
    }

    @Override
    public void unregisterCommand(String name) {
        super.unregisterCommand(name);
        proxyServer.getCommandManager().unregister(name);
    }

    @Override
    public ProxyServer getPlatform() {
        return proxyServer;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        plugin.getExecutorService().shutdown();
    }

    @Override
    public VelocitySource wrapSender(Object sender) {
        return new VelocitySource((CommandSource) sender);
    }
}