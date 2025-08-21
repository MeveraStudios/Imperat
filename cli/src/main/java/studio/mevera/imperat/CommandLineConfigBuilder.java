package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.util.TypeWrap;

import java.io.InputStream;
import java.io.PrintStream;

public final class CommandLineConfigBuilder extends ConfigBuilder<ConsoleSource, CommandLineImperat, CommandLineConfigBuilder> {

    private final InputStream inputStream;

    CommandLineConfigBuilder(InputStream inputStream) {
        this.inputStream = inputStream;
        config.registerSourceResolver(PrintStream.class, (consoleSource, ctx) -> consoleSource.origin());
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<ConsoleSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<ConsoleSource>>() {}.getType(),
                (ctx, paramElement)-> CommandHelp.create(ctx)
        );
    }
    
    /**
     * Builds and returns a configured CommandLineImperat instance.
     *
     * @return the CommandLineImperat instance
     */
    @Override
    public @NotNull CommandLineImperat build() {
        return new CommandLineImperat(inputStream, this.config);
    }
}