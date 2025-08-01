package dev.velix.imperat;

import dev.velix.imperat.context.ExecutionContext;
import dev.velix.imperat.util.TypeWrap;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.PrintStream;

public final class CommandLineConfigBuilder extends ConfigBuilder<ConsoleSource, CommandLineImperat, CommandLineConfigBuilder> {

    private final InputStream inputStream;

    CommandLineConfigBuilder(InputStream inputStream) {
        this.inputStream = inputStream;
        config.registerSourceResolver(PrintStream.class, ConsoleSource::origin);
        registerContextResolvers();
    }
    
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<ConsoleSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
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