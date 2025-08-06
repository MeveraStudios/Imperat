package studio.mevera.imperat;

import net.minestom.server.ServerProcess;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.OnlyPlayerAllowedException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.util.TypeWrap;

public final class MinestomConfigBuilder extends ConfigBuilder<MinestomSource, MinestomImperat, MinestomConfigBuilder> {

    private final ServerProcess serverProcess;

    MinestomConfigBuilder(@NotNull ServerProcess serverProcess) {
        this.serverProcess = serverProcess;
        registerDefaultResolvers();
        addThrowableHandlers();
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<MinestomSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
    }

    private void registerDefaultResolvers() {
        config.registerSourceResolver(CommandSender.class, MinestomSource::origin);

        config.registerSourceResolver(Player.class, source -> {
            if (source.isConsole()) {
                throw new OnlyPlayerAllowedException();
            }
            return source.asPlayer();
        });
    }

    private void addThrowableHandlers() {
        config.setThrowableResolver(
            UnknownPlayerException.class, (exception, context) ->
                context.source().error("A player with the name '" + exception.getName() + "' is not online.")
        );
    }

    @Override
    public @NotNull MinestomImperat build() {
        return new MinestomImperat(serverProcess, config);
    }
}