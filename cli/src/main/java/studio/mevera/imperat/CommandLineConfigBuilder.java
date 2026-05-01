package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.providers.CommandSourceMapper;
import studio.mevera.imperat.util.TypeWrap;

import java.io.InputStream;
import java.io.PrintStream;

public class CommandLineConfigBuilder<S extends ConsoleCommandSource>
        extends ConfigBuilder<S, CommandLineImperat<S>, CommandLineConfigBuilder<S>> {

    private final InputStream inputStream;

    CommandLineConfigBuilder(InputStream inputStream, Class<S> sourceClass, CommandSourceMapper<ConsoleCommandSource, S> mapper) {
        super(sourceClass);
        this.inputStream = inputStream;
        config.setSourceMapper(mapper);
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

        config.registerContextArgumentProvider(InputStream.class, (ctx, paramElement) -> inputStream);
        config.registerContextArgumentProvider(PrintStream.class, (ctx, paramElement) -> ctx.source().origin());
    }

    @Override
    public @NotNull CommandLineImperat<S> build() {
        materializeDeferredDefaults();
        return new CommandLineImperat<>(inputStream, this.config);
    }
}
