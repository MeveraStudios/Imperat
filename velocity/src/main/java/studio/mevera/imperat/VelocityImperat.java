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
import studio.mevera.imperat.type.PlayerArgument;

import java.util.concurrent.ExecutorService;

/**
 * Main Imperat implementation for Velocity proxy servers.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with Velocity proxy servers. It provides command registration, execution, and management
 * specifically tailored for Velocity's architecture.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Seamless integration with Velocity's command system</li>
 *   <li>Automatic command registration and unregistration</li>
 *   <li>Built-in parameter types for Velocity objects (Players, ServerInfo, etc.)</li>
 *   <li>Exception handling for Velocity-specific scenarios</li>
 *   <li>Support for both synchronous and asynchronous command execution</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyPlugin {
 *     private VelocityImperat<MyPlugin> imperat;
 *
 *     @Subscribe
 *     public void onProxyInitialization(ProxyInitializeEvent event) {
 *         imperat = VelocityImperat.builder(this, proxyServer)
 *             .build();
 *
 *         imperat.registerCommand(MyCommand.class);
 *     }
 * }
 * }</pre>
 *
 * @param <P> the plugin class type that owns this Imperat instance
 * @since 1.0
 * @author Imperat Framework
 * @see VelocityConfigBuilder
 * @see VelocitySource
 */
public final class VelocityImperat<P> extends BaseImperat<VelocitySource> {

    private final P plugin;
    private final ProxyServer proxyServer;

    /**
     * Package-private constructor used by VelocityConfigBuilder.
     * Use {@link #builder(Object, ProxyServer)} to create instances.
     *
     * @param plugin the plugin instance
     * @param proxyServer the ProxyServer instance
     * @param config the Imperat configuration
     */
    VelocityImperat(
            @NotNull P plugin,
            @NotNull ProxyServer proxyServer,
            @NotNull ImperatConfig<VelocitySource> config
    ) {
        super(config);
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        registerDefaultResolvers();
    }

    /**
     * Creates a new configuration builder for VelocityImperat.
     * This is the recommended way to create and configure a VelocityImperat instance.
     *
     * @param <P> the plugin class type
     * @param plugin the plugin instance that will own this Imperat instance
     * @param proxyServer the Velocity ProxyServer instance
     * @return a new VelocityConfigBuilder for further configuration
     */
    public static <P> VelocityConfigBuilder<P> builder(@NotNull P plugin, @NotNull ProxyServer proxyServer) {
        return new VelocityConfigBuilder<>(plugin, proxyServer);
    }

    private void registerDefaultResolvers() {
        // Register Player and other source/value resolvers
        config.registerArgType(Player.class, new PlayerArgument(proxyServer));

        // Define custom exception handling for unknown players
        config.setThrowableResolver(
                UnknownPlayerException.class, (exception, context) ->
                                                      context.source().error("A player with the name '" + exception.getInput()
                                                                                     + "' doesn't seem to be online")
        );

        // Register source resolver for Player
        config.registerSourceResolver(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    @Override
    public void registerSimpleCommand(Command<VelocitySource> command) {
        super.registerSimpleCommand(command);
        CommandManager manager = proxyServer.getCommandManager();
        try {
            InternalVelocityCommand<P> internalCmd = new InternalVelocityCommand<>(this, command, manager);
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


    public @NotNull P getPlugin() {
        return plugin;
    }

    @Override
    public void shutdownPlatform() {
        proxyServer.getPluginManager().fromInstance(plugin)
                .map(PluginContainer::getExecutorService)
                .ifPresent(ExecutorService::shutdown);
    }

    @Override
    public VelocitySource createDummySender() {
        return new VelocitySource(proxyServer.getConsoleCommandSource());
    }

    @Override
    public VelocitySource wrapSender(Object sender) {
        return new VelocitySource((CommandSource) sender);
    }
}