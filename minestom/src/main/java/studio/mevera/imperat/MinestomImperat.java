package studio.mevera.imperat;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;

import java.util.List;

public final class MinestomImperat extends BaseImperat<MinestomSource> {

    private final ServerProcess serverProcess;

    public static MinestomConfigBuilder builder(@NotNull ServerProcess serverProcess) {
        return new MinestomConfigBuilder(serverProcess);
    }

    MinestomImperat(@NotNull ServerProcess serverProcess, @NotNull ImperatConfig<MinestomSource> config) {
        super(config);
        this.serverProcess = serverProcess;
    }


    /**
     * @return the platform of the module
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
