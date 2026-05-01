package studio.mevera.imperat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.adventure.AdventureProvider;
import studio.mevera.imperat.adventure.BungeeAdventure;
import studio.mevera.imperat.adventure.EmptyAdventure;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.BungeePermissionChecker;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.BungeeResponseKey;
import studio.mevera.imperat.type.ProxiedPlayerArgument;
import studio.mevera.imperat.type.ServerInfoArgument;
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
 *   <li>CommandSource resolvers for type-safe command source handling</li>
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
public class BungeeConfigBuilder<S extends BungeeCommandSource>
        extends ConfigBuilder<S, BungeeImperat<S>, BungeeConfigBuilder<S>> {

    @SuppressWarnings("rawtypes")
    private final static BungeePermissionChecker DEFAULT_PERMISSION_RESOLVER = new BungeePermissionChecker();
    private final Plugin plugin;

    private AdventureProvider<CommandSender> adventureProvider;

    @SuppressWarnings({"unchecked", "rawtypes"}) BungeeConfigBuilder(
            Plugin plugin,
            Class<S> sourceClass,
            CommandSourceMapper<BungeeCommandSource, S> mapper,
            @Nullable AdventureProvider<CommandSender> adventureProvider
    ) {
        super(sourceClass);
        this.plugin = plugin;
        this.adventureProvider = adventureProvider;
        config.setSourceMapper(mapper);
        config.setPermissionResolver((BungeePermissionChecker) DEFAULT_PERMISSION_RESOLVER);
        registerBungeeResponses();
        registerSourceResolvers();
        registerValueResolvers();
        registerContextResolvers();
    }

    private void registerContextResolvers() {
        // Type-literal-keyed defaults deferred until build() so the
        // sourceClass token is correct at lookup time.
        deferredDefaults.add(cfg -> {
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(ExecutionContext.class, sourceClass).getType(),
                    (ctx, paramElement) -> ctx
            );
            cfg.registerContextArgumentProvider(
                    TypeWrap.ofParameterized(CommandHelp.class, sourceClass).getType(),
                    (ctx, paramElement) -> CommandHelp.create(ctx)
            );
        });

        // Enhanced context resolvers similar to Velocity
        config.registerContextArgumentProvider(Plugin.class, (ctx, paramElement) -> plugin);
        config.registerContextArgumentProvider(ProxyServer.class, (ctx, paramElement) -> ProxyServer.getInstance());
        config.registerContextArgumentProvider(ServerInfo.class, (ctx, paramElement) -> {
            BungeeCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(BungeeResponseKey.ONLY_PLAYER);
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
        // v4: SourceProviderRegistry deleted. Cross-source-type @Execute
        // params resolve via assignability against the canonical S
        // (or its origin()) plus the ContextArgumentProvider below for
        // the gating-required ProxiedPlayer view.
        config.registerContextArgumentProvider(ProxiedPlayer.class, (ctx, p) -> {
            BungeeCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(BungeeResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    private void registerBungeeResponses() {
        this.visit(ImperatConfig::getResponseRegistry, responseRegistry -> {
            // Register responses for Bungee-specific exceptions
            responseRegistry.registerResponse(
                    BungeeResponseKey.ONLY_PLAYER,
                    () -> "Only players can do this!"
            );

            responseRegistry.registerResponse(
                    BungeeResponseKey.ONLY_CONSOLE,
                    () -> "Only console can do this!"
            );

            responseRegistry.registerResponse(
                    BungeeResponseKey.UNKNOWN_PLAYER,
                    () -> "A player with the name '%input%' doesn't seem to be online", "input"
            );

            responseRegistry.registerResponse(
                    BungeeResponseKey.UNKNOWN_SERVER,
                    () -> "A server with the name '%input%' doesn't seem to exist", "input"
            );
        });

    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerValueResolvers() {
        config.registerArgType(ProxiedPlayer.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new ProxiedPlayerArgument());
        config.registerArgType(ServerInfo.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new ServerInfoArgument());
    }

    @Override
    public @NotNull BungeeImperat<S> build() {
        if (this.adventureProvider == null) {
            this.adventureProvider = this.loadAdventure();
        }
        materializeDeferredDefaults();
        return new BungeeImperat<>(plugin, adventureProvider, this.config);
    }
}