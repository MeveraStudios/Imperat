package studio.mevera.imperat;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.util.TypeWrap;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * Configuration builder for CommandLineImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a command-line interface environment.
 *
 * <p>The builder automatically sets up:</p>
 * <ul>
 *   <li>Basic source resolvers for PrintStream handling</li>
 *   <li>Context resolvers for dependency injection</li>
 *   <li>Simple configuration suitable for CLI applications</li>
 *   <li>No complex platform-specific features (kept minimal)</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * CommandLineImperat imperat = CommandLineImperat.builder(System.in)
 *     .build();
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see CommandLineImperat
 */
public final class CommandLineConfigBuilder extends ConfigBuilder<ConsoleSource, CommandLineImperat, CommandLineConfigBuilder> {

    private final InputStream inputStream;

    /**
     * Package-private constructor used by CommandLineImperat.builder().
     *
     * @param inputStream the input stream for command reading
     */
    CommandLineConfigBuilder(InputStream inputStream) {
        this.inputStream = inputStream;
        registerSourceResolvers();
        registerContextResolvers();
    }
    
    /**
     * Registers source resolvers for type-safe command source handling.
     * For CLI applications, this sets up PrintStream source resolution.
     */
    private void registerSourceResolvers() {
        config.registerSourceResolver(PrintStream.class, (consoleSource, ctx) -> consoleSource.origin());
    }
    
    /**
     * Registers context resolvers for automatic dependency injection in commands.
     * This allows command methods to receive CLI-specific objects as parameters.
     */
    private void registerContextResolvers() {
        config.registerContextResolver(
                new TypeWrap<ExecutionContext<ConsoleSource>>() {}.getType(),
                (ctx, paramElement)-> ctx
        );
        config.registerContextResolver(
                new TypeWrap<CommandHelp<ConsoleSource>>() {}.getType(),
                (ctx, paramElement)-> CommandHelp.create(ctx)
        );
        
        // Enhanced context resolvers similar to Velocity (limited for CLI)
        config.registerContextResolver(InputStream.class, (ctx, paramElement) -> inputStream);
        config.registerContextResolver(PrintStream.class, (ctx, paramElement) -> ctx.source().origin());
    }
    
    /**
     * Builds the configured CommandLineImperat instance.
     *
     * @return a new CommandLineImperat instance with the specified configuration
     */
    @Override
    public @NotNull CommandLineImperat build() {
        return new CommandLineImperat(inputStream, this.config);
    }
}