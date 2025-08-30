package studio.mevera.imperat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;

import java.util.List;

/**
 * Main Imperat implementation for Minestom servers.
 * This class serves as the primary entry point for integrating the Imperat command framework
 * with Minestom servers, providing modern Minecraft server command management capabilities.
 *
 * <p>Key Features:</p>
 * <ul>
 *   <li>Full integration with Minestom's command system</li>
 *   <li>Native Adventure API support for rich text messaging</li>
 *   <li>Modern Minecraft server architecture support</li>
 *   <li>High-performance command execution</li>
 *   <li>Built-in parameter types for Minestom objects</li>
 *   <li>Automatic command registration and cleanup</li>
 * </ul>
 *
 * <p>Usage Example:</p>
 * <pre>{@code
 * public class MyMinestomServer {
 *     private MinestomImperat imperat;
 *
 *     public void initialize(ServerProcess serverProcess) {
 *         imperat = MinestomImperat.builder(serverProcess)
 *             .build();
 *
 *         imperat.registerCommand(MyCommand.class);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0
 * @author Imperat Framework
 * @see MinestomConfigBuilder
 * @see MinestomSource
 */
public final class MinestomImperat extends BaseImperat<MinestomSource> {

    private final ServerProcess serverProcess;

    /**
     * Creates a new configuration builder for MinestomImperat.
     * This is the recommended way to create and configure a MinestomImperat instance.
     *
     * @param serverProcess the Minestom ServerProcess instance
     * @return a new MinestomConfigBuilder for further configuration
     */
    public static MinestomConfigBuilder builder(@NotNull ServerProcess serverProcess) {
        return new MinestomConfigBuilder(serverProcess);
    }

    /**
     * Package-private constructor used by MinestomConfigBuilder.
     * Use {@link #builder(ServerProcess)} to create instances.
     *
     * @param serverProcess the Minestom ServerProcess instance
     * @param config the Imperat configuration
     */
    MinestomImperat(@NotNull ServerProcess serverProcess, @NotNull ImperatConfig<MinestomSource> config) {
        super(config);
        this.serverProcess = serverProcess;
    }

    /**
     * Gets the platform object for this implementation.
     * For Minestom, this returns the ServerProcess.
     *
     * @return the Minestom ServerProcess instance
     */
    @Override
    public ServerProcess getPlatform() {
        return serverProcess;
    }

    /**
     * Shuts down the platform
     */
    @Override
    public void shutdownPlatform() {
        serverProcess.stop();
    }

    /**
     * Wraps the sender into a built-in command-sender valueType
     *
     * @param sender the sender's actual value
     * @return the wrapped command-sender valueType
     */
    @Override
    public MinestomSource wrapSender(Object sender) {
        if (!(sender instanceof CommandSender commandSender)) {
            throw new IllegalArgumentException("platform sender is not of valueType `" + CommandSender.class.getName() + "`");
        }
        return new MinestomSource(commandSender);
    }

    /**
     * Registering a command into the dispatcher
     *
     * @param command the command to register
     */
    @Override
    public void registerCommand(Command<MinestomSource> command) {
        super.registerCommand(command);
        MinecraftServer.getCommandManager().register(new InternalMinestomCommand(this, command));
    }

    /**
     * Unregisters a command from the internal registry
     *
     * @param name the name of the command to unregister
     */
    @Override
    public void unregisterCommand(String name) {
        super.unregisterCommand(name);
        MinecraftServer.getCommandManager().getCommands().stream()
            .filter(cmd -> cmd.getName().equalsIgnoreCase(name) || List.of(cmd.getAliases()).contains(name.toLowerCase()))
            .forEach(MinecraftServer.getCommandManager()::unregister);
    }
}
