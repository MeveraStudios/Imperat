package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.ProxyVersion;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyConsoleAllowedException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.exception.UnknownServerException;
import studio.mevera.imperat.type.ParameterPlayer;
import studio.mevera.imperat.type.ParmeterServerInfo;
import studio.mevera.imperat.util.TypeWrap;

/**
 * Configuration builder for VelocityImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a Velocity proxy environment.
 *
 * <p>The builder automatically sets up:</p>
 * <ul>
 *   <li>Velocity-specific parameter types (Player, ServerInfo)</li>
 *   <li>Exception handlers for common Velocity scenarios</li>
 *   <li>Source resolvers for type-safe command source handling</li>
 *   <li>Context resolvers for dependency injection</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * VelocityImperat<MyPlugin> imperat = VelocityImperat.builder(plugin, proxyServer)
 *     .build();
 * }</pre>
 *
 * @param <P> the plugin class type
 * @since 1.0
 * @author Imperat Framework
 * @see VelocityImperat
 */
public final class VelocityConfigBuilder<P> extends ConfigBuilder<VelocitySource, VelocityImperat<P>, VelocityConfigBuilder<P>> {

    private final P plugin;
    private final ProxyServer proxyServer;

    /**
     * Package-private constructor used by VelocityImperat.builder().
     *
     * @param plugin the plugin instance
     * @param proxyServer the ProxyServer instance
     */
    VelocityConfigBuilder(@NotNull P plugin, @NotNull ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        addThrowableHandlers();
        registerSourceResolvers();
        registerParameterTypes();
        registerContextResolvers();
        
    }
    
    /**
     * Registers context resolvers for automatic dependency injection in commands.
     * This allows command methods to receive Velocity-specific objects as parameters.
     */
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<VelocitySource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<VelocitySource>>() {}.getType(),
                (ctx, paramElement)-> CommandHelp.create(ctx)
        );
        
        config.registerContextResolver(ProxyConfig.class, (ctx, paramElement) -> proxyServer.getConfiguration());
        config.registerContextResolver(ProxyVersion.class, (ctx, paramElement) -> proxyServer.getVersion());
        config.registerContextResolver(ServerInfo.class, (ctx, paramElement) -> {
            VelocitySource source = ctx.source();
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            Player player = source.asPlayer();
            return player.getCurrentServer()
                    .map(serverConnection -> serverConnection.getServer().getServerInfo())
                    .orElseThrow(()-> new IllegalStateException("Source is not connected to any server"));
        });
        config.registerContextResolver(PluginContainer.class, (ctx, paramElement) -> proxyServer.getPluginManager().fromInstance(plugin).orElseThrow(()-> new IllegalStateException("Cannot get plugin container")));
    }

    /**
     * Registers source resolvers for type-safe command source handling.
     * This enables automatic casting and validation of command sources.
     */
    private void registerSourceResolvers() {
        config.registerSourceResolver(ConsoleCommandSource.class, (velocitySource, ctx) -> {
            if (!velocitySource.isConsole()) {
                throw new OnlyConsoleAllowedException(ctx);
            }
            return velocitySource.asConsole();
        });
        
        config.registerSourceResolver(CommandSource.class, (velocitySource, ctx) -> velocitySource.origin());

        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            return source.asPlayer();
        });
    }

    /**
     * Registers exception handlers for common Velocity command scenarios.
     * This provides user-friendly error messages for various error conditions.
     */
    private void addThrowableHandlers() {
        config.setThrowableResolver(OnlyPlayerAllowedException.class, (ex, context)-> {
            context.source().error("Only players can do this!");
        });
        
        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, context) ->
                context.source().error("A player with the name '" + exception.getInput() + "' doesn't seem to be online")
        );
        
        //resolve for unknownserverexception
        config.setThrowableResolver(
            UnknownServerException.class, (exception, context) ->
                context.source().error("A server with the name '" + exception.getInput() + "' doesn't seem to exist")
        );
    }

    /**
     * Registers parameter types for Velocity-specific objects.
     * This enables commands to accept Players, ServerInfo, etc. as parameters.
     */
    private void registerParameterTypes() {
        config.registerParamType(Player.class, new ParameterPlayer(proxyServer));
        config.registerParamType(ServerInfo.class, new ParmeterServerInfo(proxyServer));
    }

    /**
     * Builds the configured VelocityImperat instance.
     *
     * @return a new VelocityImperat instance with the specified configuration
     */
    @Override
    public @NotNull VelocityImperat<P> build() {
        return new VelocityImperat<>(plugin, proxyServer, this.config);
    }
}