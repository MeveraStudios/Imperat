package studio.mevera.imperat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.adventure.BungeeAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyConsoleAllowedException;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.exception.UnknownServerException;
import studio.mevera.imperat.resolvers.BungeePermissionChecker;
import studio.mevera.imperat.type.ParameterProxiedPlayer;
import studio.mevera.imperat.type.ParameterServerInfo;
import studio.mevera.imperat.util.TypeWrap;
import studio.mevera.imperat.util.reflection.Reflections;

/**
 * Configuration builder for BungeeImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a BungeeCord proxy environment.
 *
 * <p>The builder automatically sets up:</p>
 * <ul>
 *   <li>BungeeCord-specific parameter types (ProxiedPlayer, ServerInfo)</li>
 *   <li>Exception handlers for common BungeeCord scenarios</li>
 *   <li>Source resolvers for type-safe command source handling</li>
 *   <li>Adventure API integration with automatic detection</li>
 *   <li>Cross-server functionality support</li>
 *   <li>Permission system integration</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * BungeeImperat imperat = BungeeImperat.builder(plugin)
 *     .build();
 * }</pre>
 *
 * @author Imperat Framework
 * @see BungeeImperat
 * @since 1.0
 */
public final class BungeeConfigBuilder extends ConfigBuilder<BungeeSource, BungeeImperat, BungeeConfigBuilder> {

    private final static BungeePermissionChecker DEFAULT_PERMISSION_RESOLVER = new BungeePermissionChecker();
    private final Plugin plugin;

    private AdventureProvider<CommandSender> adventureProvider;

    BungeeConfigBuilder(Plugin plugin, @Nullable AdventureProvider<CommandSender> adventureProvider) {
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        config.setPermissionResolver(DEFAULT_PERMISSION_RESOLVER);
        addThrowableHandlers();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }

    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<BungeeSource>>() {}.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<BungeeSource>>() {}.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );

        // Enhanced context resolvers similar to Velocity
        config.registerContextResolver(Plugin.class, (ctx, paramElement) -> plugin);
        config.registerContextResolver(ProxyServer.class, (ctx, paramElement) -> ProxyServer.getInstance());
        config.registerContextResolver(ServerInfo.class, (ctx, paramElement) -> {
            BungeeSource source = ctx.source();
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            ProxiedPlayer player = source.asPlayer();
            return player.getServer() != null ? player.getServer().getInfo() : null;
        });
    }

    public void setAdventureProvider(AdventureProvider<CommandSender> adventureProvider) {
        this.adventureProvider = adventureProvider;
    }

    private AdventureProvider<CommandSender> loadAdventure() {
        if (Reflections.findClass("net.kyori.adventure.platform.bungeecord.BungeeAudiences")) {
            return new BungeeAdventure(plugin);
        }
        return new EmptyAdventure<>();
    }

    private void registerSourceResolvers() {
        config.registerSourceResolver(AdventureSource.class, (bungeeSource, ctx) -> bungeeSource);
        config.registerSourceResolver(CommandSender.class, (bungeeSource, ctx) -> bungeeSource.origin());
        config.registerSourceResolver(ProxiedPlayer.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException(ctx);
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(
                OnlyPlayerAllowedException.class,
                (ex, context) -> context.source().error("Only players can do this!")
        );

        // Enhanced exception handling similar to Velocity
        config.setThrowableResolver(
                OnlyConsoleAllowedException.class,
                (ex, context) -> context.source().error("Only console can do this!")
        );

        config.setThrowableResolver(
                UnknownPlayerException.class,
                (exception, context) ->
                        context.source().error("A player with the name '" + exception.getName() + "' doesn't seem to be online")
        );

        // Enhanced server exception handling similar to Velocity
        config.setThrowableResolver(
                UnknownServerException.class,
                (exception, context) ->
                        context.source().error("A server with the name '" + exception.getInput() + "' doesn't seem to exist")
        );
    }

    private void registerValueResolvers() {
        config.registerParamType(ProxiedPlayer.class, new ParameterProxiedPlayer());
        // Enhanced parameter types similar to Velocity
        config.registerParamType(ServerInfo.class, new ParameterServerInfo());
    }

    @Override
    public @NotNull BungeeImperat build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        return new BungeeImperat(plugin, adventureProvider, this.config);
    }
}