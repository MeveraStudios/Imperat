package studio.mevera.imperat;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.VelocityResponseKey;
import studio.mevera.imperat.type.PlayerArgument;

import java.util.concurrent.ExecutorService;

public final class VelocityImperat<P, S extends VelocityCommandSource> extends BaseImperat<S> {

    private final P plugin;
    private final ProxyServer proxyServer;

    VelocityImperat(
            @NotNull P plugin,
            @NotNull ProxyServer proxyServer,
            @NotNull ImperatConfig<S> config
    ) {
        super(config);
        this.plugin = plugin;
        this.proxyServer = proxyServer;
        registerDefaultResolvers();
    }

    public static <P> VelocityConfigBuilder<P, VelocityCommandSource> builder(@NotNull P plugin, @NotNull ProxyServer proxyServer) {
        return new VelocityConfigBuilder<>(plugin, proxyServer, VelocityCommandSource.class, CommandSourceMapper.identity());
    }

    public static <P, S extends VelocityCommandSource> VelocityConfigBuilder<P, S> builder(
            @NotNull P plugin, @NotNull ProxyServer proxyServer, Class<S> sourceClass, CommandSourceMapper<VelocityCommandSource, S> mapper
    ) {
        return new VelocityConfigBuilder<>(plugin, proxyServer, sourceClass, mapper);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerDefaultResolvers() {
        config.registerArgType(Player.class, (studio.mevera.imperat.command.arguments.type.ArgumentType) new PlayerArgument(proxyServer));

        // Player as method param resolved via ContextArgumentProvider
        // (gating-aware) — replaces the deleted SourceProvider chain.
        config.registerContextArgumentProvider(Player.class, (ctx, p) -> {
            VelocityCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(VelocityResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    @Override
    public void registerSimpleCommand(Command<S> command) {
        super.registerSimpleCommand(command);
        CommandManager manager = proxyServer.getCommandManager();
        try {
            InternalVelocityCommand<P, S> internalCmd = new InternalVelocityCommand<>(this, command, manager);
            manager.register(internalCmd.getMeta(), internalCmd);
        } catch (Exception ex) {
            config.handleExecutionError(ex, null, VelocityImperat.class, "registerCommand");
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S createDummySender() {
        VelocityCommandSource platform = new VelocityCommandSource(proxyServer.getConsoleCommandSource());
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public S wrapSender(Object sender) {
        VelocityCommandSource platform = new VelocityCommandSource((CommandSource) sender);
        CommandSourceMapper mapper = config().sourceMapper();
        return (S) mapper.wrap(platform);
    }
}
