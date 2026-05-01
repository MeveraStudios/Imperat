package studio.mevera.imperat;

import net.minestom.server.ServerProcess;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.ResponseException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.responses.MinestomResponseKey;
import studio.mevera.imperat.util.TypeWrap;

public class MinestomConfigBuilder<S extends MinestomCommandSource>
        extends ConfigBuilder<S, MinestomImperat<S>, MinestomConfigBuilder<S>> {

    private final ServerProcess serverProcess;

    MinestomConfigBuilder(
            @NotNull ServerProcess serverProcess,
            Class<S> sourceClass,
            CommandSourceMapper<MinestomCommandSource, S> mapper
    ) {
        super(sourceClass);
        this.serverProcess = serverProcess;
        config.setSourceMapper(mapper);
        this.permissionChecker((src, perm) -> perm == null || src.isConsole());
        registerDefaultResolvers();
        addThrowableHandlers();
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

        config.registerContextArgumentProvider(ServerProcess.class, (ctx, paramElement) -> serverProcess);
    }

    private void registerDefaultResolvers() {
        // v4: SourceProviderRegistry deleted. ConsoleSender / Player gating
        // moves to ContextArgumentProvider — assignability handles
        // CommandSender / AdventureCommandSource directly via covariance
        // through `S extends MinestomCommandSource`.
        config.registerContextArgumentProvider(ConsoleSender.class, (ctx, p) -> {
            MinestomCommandSource source = ctx.source();
            if (!source.isConsole()) {
                throw ResponseException.of(MinestomResponseKey.ONLY_CONSOLE);
            }
            return (ConsoleSender) source.origin();
        });

        config.registerContextArgumentProvider(Player.class, (ctx, p) -> {
            MinestomCommandSource source = ctx.source();
            if (source.isConsole()) {
                throw ResponseException.of(MinestomResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setErrorHandler(
                UnknownPlayerException.class,
                (exception, context) -> context.source().error("A player with the name '" + exception.getName() + "' is not online.")
        );
    }

    @Override
    public @NotNull MinestomImperat<S> build() {
        materializeDeferredDefaults();
        return new MinestomImperat<>(serverProcess, config);
    }
}
