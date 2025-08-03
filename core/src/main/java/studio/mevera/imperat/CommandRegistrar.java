package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;

import java.util.Collection;

public sealed interface CommandRegistrar<S extends Source> permits Imperat {

    /**
     * Registers a command into the dispatcher
     *
     * @param command the command to register
     */
    void registerCommand(Command<S> command);

    /**
     * Registers some commands into the dispatcher
     *
     * @param commands the commands to register
     */
    @SuppressWarnings("all")
    default void registerCommands(Command<S>... commands) {
        for (final var command : commands) {
            this.registerCommand(command);
        }
    }

    /**
     * Registers a command class built by the
     * annotations using a parser
     *
     * @param command the annotated command instance to parse
     */
    void registerCommand(Object command);

    /**
     * Registers some commands into the dispatcher
     * annotations using a parser
     *
     * @param commands the commands to register
     */
    default void registerCommands(Object... commands) {
        for (final var command : commands) {
            this.registerCommand(command);
        }
    }

    /**
     * Unregisters a command from the internal registry
     *
     * @param name the name of the command to unregister
     */
    void unregisterCommand(String name);

    /**
     * Unregisters all commands from the internal registry
     */
    void unregisterAllCommands();

    /**
     * @param name the name/alias of the command
     * @return fetches {@link Command} with specific name
     */
    @Nullable
    Command<S> getCommand(final String name);

    /**
     * @param parameter the parameter
     * @return the command from the parameter's name
     */
    default @Nullable Command<S> getCommand(final CommandParameter<S> parameter) {
        return getCommand(parameter.name());
    }

    /**
     * @param owningCommand the command owning this sub-command
     * @param name          the name of the subcommand you're looking for
     * @return the subcommand of a command
     */
    @Nullable
    Command<S> getSubCommand(final String owningCommand, final String name);

    /**
     * Gets all registered commands
     *
     * @return the registered commands
     */
    Collection<? extends Command<S>> getRegisteredCommands();

}
