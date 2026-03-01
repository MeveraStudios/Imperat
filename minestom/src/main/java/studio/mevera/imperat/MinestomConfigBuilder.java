package studio.mevera.imperat;

import net.minestom.server.ServerProcess;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.adventure.AdventureSource;
import studio.mevera.imperat.command.tree.help.CommandHelp;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.UnknownPlayerException;
import studio.mevera.imperat.responses.MinestomResponseKey;
import studio.mevera.imperat.util.TypeWrap;

/**
 * Configuration builder for MinestomImperat instances.
 * This builder provides a fluent API for configuring and customizing the behavior
 * of Imperat commands in a Minestom server environment.
 *
 * <p>The builder automatically sets up:</p>
 * <ul>
 *   <li>Minestom-specific source resolvers for type-safe command source handling</li>
 *   <li>Exception handlers for common Minestom scenarios</li>
 *   <li>CommandContext resolvers for dependency injection</li>
 *   <li>Integration with Minestom's modern architecture</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * MinestomImperat imperat = MinestomImperat.builder(serverProcess)
 *     .build();
 * }</pre>
 *
 * @author Imperat Framework
 * @see MinestomImperat
 * @since 1.0
 */
public final class MinestomConfigBuilder extends ConfigBuilder<MinestomSource, MinestomImperat, MinestomConfigBuilder> {

    private final ServerProcess serverProcess;

    /**
     * Package-private constructor used by MinestomImperat.builder().
     *
     * @param serverProcess the Minestom ServerProcess instance
     */
    MinestomConfigBuilder(@NotNull ServerProcess serverProcess) {
        this.serverProcess = serverProcess;
        this.permissionChecker((src, perm) -> perm == null || src.isConsole());
        registerDefaultResolvers();
        addThrowableHandlers();
        registerContextResolvers();
    }

    /**
     * Registers context resolvers for automatic dependency injection in commands.
     * This allows command methods to receive Minestom-specific objects as parameters.
     */
    private void registerContextResolvers() {
        config.registerContextArgumentProvider(
                new TypeWrap<ExecutionContext<MinestomSource>>() {
                }.getType(),
                (ctx, paramElement) -> ctx
        );
        config.registerContextArgumentProvider(
                new TypeWrap<CommandHelp<MinestomSource>>() {
                }.getType(),
                (ctx, paramElement) -> CommandHelp.create(ctx)
        );

        // Enhanced context resolvers similar to Velocity
        config.registerContextArgumentProvider(ServerProcess.class, (ctx, paramElement) -> serverProcess);
    }

    /**
     * Registers source resolvers for type-safe command source handling.
     * This enables automatic casting and validation of command sources.
     */
    private void registerDefaultResolvers() {
        config.registerSourceProvider(CommandSender.class, (minestomSource, ctx) -> minestomSource.origin());

        // Enhanced source resolver for console similar to Velocity
        config.registerSourceProvider(AdventureSource.class, (minestomSource, ctx) -> minestomSource);
        config.registerSourceProvider(ConsoleSender.class, (minestomSource, ctx) -> {
            if (!minestomSource.isConsole()) {
                throw new CommandException(MinestomResponseKey.ONLY_CONSOLE);
            }
            return (ConsoleSender) minestomSource.origin();
        });

        config.registerSourceProvider(Player.class, (source, ctx) -> {
            if (source.isConsole()) {
                throw new CommandException(MinestomResponseKey.ONLY_PLAYER);
            }
            return source.asPlayer();
        });
    }

    /**
     * Registers exception handlers for common Minestom command scenarios.
     * This provides user-friendly error messages for various error conditions.
     */
    private void addThrowableHandlers() {
        config.setThrowableResolver(
                UnknownPlayerException.class,
                (exception, context) -> context.source().error("A player with the name '" + exception.getName() + "' is not online.")
        );
    }

    /**
     * Builds the configured MinestomImperat instance.
     *
     * @return a new MinestomImperat instance with the specified configuration
     */
    @Override
    public @NotNull MinestomImperat build() {
        return new MinestomImperat(serverProcess, config);
    }
}