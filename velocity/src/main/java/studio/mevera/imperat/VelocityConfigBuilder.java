package studio.mevera.imperat;

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
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.ResponseRegistry;
import studio.mevera.imperat.responses.VelocityResponseKey;
import studio.mevera.imperat.type.PlayerArgument;
import studio.mevera.imperat.type.ServerInfoArgument;
import studio.mevera.imperat.util.TypeWrap;

public class VelocityConfigBuilder<P, S extends VelocityCommandSource>
        extends ConfigBuilder<S, VelocityImperat<P, S>, VelocityConfigBuilder<P, S>> {

    private final P plugin;
    private final ProxyServer proxyServer;

    VelocityConfigBuilder(
            @NotNull P plugin,
            @NotNull ProxyServer proxyServer,
            Class<S> sourceClass,
            CommandSourceMapper<VelocityCommandSource, S> mapper
    ) {
        super(sourceClass);
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        config.setSourceMapper(mapper);
        this.permissionChecker((src, perm) -> {
            if (perm == null || src.isConsole()) {
                return true;
            }
            return src.asPlayer().hasPermission(perm);
        });
        registerVelocityResponses();
        registerSourceResolvers();
        registerArgumentTypes();
        registerContextResolvers();
    }

    private void registerContextResolvers() {
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

        config.registerContextArgumentProvider(ProxyConfig.class, (ctx, paramElement) -> proxyServer.getConfiguration());
        config.registerContextArgumentProvider(ProxyVersion.class, (ctx, paramElement) -> proxyServer.getVersion());
        config.registerContextArgumentProvider(ServerInfo.class, (ctx, paramElement) -> {
            VelocityCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(VelocityResponseKey.ONLY_PLAYER);
            }
            Player player = source.asPlayer();
            return player.getCurrentServer()
                           .map(serverConnection -> serverConnection.getServer().getServerInfo())
                           .orElseThrow(() -> new IllegalStateException("CommandSource is not connected to any server"));
        });
        config.registerContextArgumentProvider(PluginContainer.class,
                (ctx, paramElement) -> proxyServer.getPluginManager().fromInstance(plugin).orElseThrow(
                () -> new IllegalStateException("Cannot get plugin container")));
    }

    private void registerSourceResolvers() {
        // v4: SourceProviderRegistry deleted. Cross-source-type @Execute
        // params resolve via assignability. Player / ConsoleCommandSource
        // gating goes through ContextArgumentProvider.
        config.registerContextArgumentProvider(Player.class, (ctx, p) -> {
            VelocityCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(VelocityResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
        config.registerContextArgumentProvider(ConsoleCommandSource.class, (ctx, p) -> {
            VelocityCommandSource source = ctx.source();
            if (!source.isConsole()) {
                throw ResponseException.of(VelocityResponseKey.ONLY_CONSOLE);
            }
            return source.asConsole();
        });
    }

    private void registerVelocityResponses() {
        this.visit(ImperatConfig::getResponseRegistry, responseRegistry -> {
            responseRegistry.registerResponse(
                    VelocityResponseKey.ONLY_PLAYER,
                    () -> "Only players can do this!"
            );
            responseRegistry.registerResponse(
                    VelocityResponseKey.ONLY_CONSOLE,
                    () -> "Only console can do this!"
            );
            registerInputResponse(responseRegistry, VelocityResponseKey.UNKNOWN_PLAYER,
                    "A player with the name '%input%' doesn't seem to be online");
            registerInputResponse(responseRegistry, VelocityResponseKey.UNKNOWN_SERVER,
                    "A server with the name '%input%' doesn't seem to exist");
        });
    }

    private void registerInputResponse(ResponseRegistry responseRegistry, VelocityResponseKey responseKey, String message) {
        responseRegistry.registerResponse(
                responseKey,
                () -> message,
                "input"
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerArgumentTypes() {
        config.registerArgType(Player.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new PlayerArgument(proxyServer));
        config.registerArgType(ServerInfo.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new ServerInfoArgument(proxyServer));
    }

    @Override
    public @NotNull VelocityImperat<P, S> build() {
        materializeDeferredDefaults();
        return new VelocityImperat<>(plugin, proxyServer, this.config);
    }
}
