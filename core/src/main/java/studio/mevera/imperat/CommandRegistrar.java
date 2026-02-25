package studio.mevera.imperat;

import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.exception.AmbiguousCommandException;

import java.util.Collection;

public sealed interface CommandRegistrar<S extends Source> permits Imperat {

    /**
     * Registering a command into the global registry,
     * it will check for ambiguity with other commands and their tree before registering,
     * if an ambiguity is detected it will throw an {@link AmbiguousCommandException}
     *
     * @param command the command to register
     */
    void registerSimpleCommand(Command<S> command);

    /**
     * Registers some commands into the dispatcher
     *
     * @param commands the commands to register
     */
    @SuppressWarnings("all")
    default void registerCommands(Command<S>... commands) {
        for (final var command : commands) {
            this.registerSimpleCommand(command);
        }
    }

    /**
     * Registers a command class built by the
     * annotations using a parser
     *
     * @param command the annotated command instance to parse
     */
    void registerCommand(Class<?> command);

    /**
     * Registers some commands into the dispatcher
     * annotations using a parser
     *
     * @param commands the commands to register
     */

    default void registerCommands(Class<?>... commands) {
        for (final var command : commands) {
            this.registerCommand(command);
        }
    }

    /**
     * Registers a command instance built by the
     * annotations using a parser
     *
     * @param commandInstance the annotated command instance to parse
     */
    void registerCommand(Object commandInstance);

    /**
     * Registers some command instances built by the
     * annotations using a parser
     *
     * @param commandInstances the annotated command instances to parse
     */
    default void registerCommands(Object... commandInstances) {
        for (var obj : commandInstances) {
            registerCommand(obj);
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
    default @Nullable Command<S> getCommand(final Argument<S> parameter) {
        return getCommand(parameter.getName());
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
