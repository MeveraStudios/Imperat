package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.types.Context;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.context.internal.ParsedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.ContextArgumentProvider;
import studio.mevera.imperat.util.ImperatDebugger;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Represents the execution context for a command, responsible for resolving and managing
 * command arguments, flags, and their values during command execution.
 *
 * <p>This interface serves as the bridge between raw command input and executable command
 * logic by:
 * <ul>
 *   <li>Resolving string inputs into typed values</li>
 *   <li>Managing command flags and their values</li>
 *   <li>Caching resolved arguments for execution</li>
 *   <li>Tracking the command execution path</li>
 * </ul>
 *
 * <p>Each {@link Command} maintains its own context, allowing subcommands to have
 * independent argument resolution while preserving the full execution chain.
 *
 * @param <S> the type of the command source/sender
 *
 * @see Command
 * @see CommandPathway
 * @see ParsedArgument
 * @see ParsedFlagArgument
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
@Context
public interface ExecutionContext<S extends Source> extends CommandContext<S> {


    /**
     * Retrieves a flag by its name if it was provided in the command input.
     *
     * @param flagName the name of the flag (without prefix)
     * @return an {@link Optional} containing the flag if present, empty otherwise
     */
    Optional<ParsedFlagArgument<S>> getFlag(String flagName);

    /**
     * Gets the resolved value of a command flag.
     *
     * @param <T> the type of the flag value
     * @param flagName the name of the flag (without prefix)
     * @return the resolved flag value, or {@code null} if:
     *         <ul>
     *           <li>The flag is a switch (no value)</li>
     *           <li>The flag wasn't provided</li>
     *           <li>The value couldn't be resolved</li>
     *         </ul>
     */
    @Nullable
    <T> T getFlagValue(String flagName);

    /**
     * Gets a resolved command argument by its parameter name.
     *
     * @param <T> the type of the argument value
     * @param name the parameter name defined in the {@link CommandPathway}
     * @return the resolved argument value, or {@code null} if not provided or couldn't be resolved
     * @see ParsedArgument
     */
    <T> @Nullable T getArgument(String name);

    /**
     * Gets a resolved argument or returns a default value if not present.
     *
     * @param <T> the type of the argument value
     * @param name the parameter name
     * @param value the default value to return if argument isn't present
     * @return the resolved argument value or the default value
     */
    default <T> @NotNull T getArgumentOr(String name, T value) {
        final T argValue = getArgument(name);
        if (argValue != null) {
            return argValue;
        }
        return value;
    }

    /**
     * Gets the raw string input for an argument by its position.
     *
     * @param index the zero-based position of the argument in the command input
     * @return the raw argument string, or {@code null} if index is out of bounds
     */
    default String getRawArgument(int index) {
        if (index >= arguments().size() || index < 0) {
            return null;
        }
        return arguments().get(index);
    }

    /**
     * Resolves the command source into a different type using the configured source resolver.
     *
     * @param <R> the target type to resolve to
     * @param type the target type class
     * @return the resolved source, never {@code null}
     * @throws CommandException if resolution fails
     * @see ImperatConfig#getSourceProviderFor(Type) (Type)
     */
    <R> @NotNull R provideSource(Type type) throws CommandException;

    /**
     * Gets an argument resolved by the context resolver system.
     *
     * @param <T> the type of the argument
     * @param type the class of the argument type
     * @return the resolved argument value, or {@code null} if not resolvable
     * @throws CommandException if resolution fails
     * @see ContextArgumentProvider
     */
    <T> @Nullable T getContextArgument(Class<T> type) throws CommandException;

    /**
     * Gets all flags that were resolved from the command input.
     *
     * @return a collection of resolved flags
     */
    Collection<? extends ParsedFlagArgument> getResolvedFlags();

    /**
     * Resolves and registers a command argument.
     *
     * @param <T> the type of the argument value
     * @param stream the stream having the current input's data.
     * @param value the resolved value.
     * @throws CommandException if resolution fails.
     */
    <T> void parseArgument(
            Cursor<S> stream,
            @Nullable T value
    ) throws CommandException;

    /**
     * Registers a resolved argument in the context.
     *
     * @param argument the resolved argument to register
     */
    void parseArgument(
            ParsedArgument<S> argument
    ) throws CommandException;

    /**
     * Registers a resolved flag in the context.
     *
     * @param flag the resolved flag to register
     */
    void resolveFlag(ParsedFlagArgument<S> flag) throws CommandException;

    /**
     * Gets the most specific command that was resolved in this context.
     *
     * @return the terminal command that will be executed
     */
    Command<S> getLastUsedCommand();

    /**
     * Gets the command usage pattern that matched the input.
     *
     * @return the detected command usage
     */
    CommandPathway<S> getDetectePathway();

    /**
     * Resolves all arguments and flags from the raw context input.
     *
     * @throws CommandException if resolution fails
     */
    void resolve() throws CommandException;

    /**
     * Gets a resolved argument by its owning command and parameter name.
     *
     * @param command the owning command
     * @param name the parameter name
     * @return the resolved argument, or {@code null} if not present
     */
    @Nullable
    ParsedArgument<S> getParsedArgument(Command<S> command, String name);

    @Nullable ParsedArgument<S> getParsedArgument(String argumentName);

    /**
     * Gets all resolved arguments for a specific command.
     *
     * @param command the owning command
     * @return a list of resolved arguments in declaration order
     */
    List<ParsedArgument<S>> getParsedArguments(Command<S> command);

    /**
     * Gets all commands in the resolution path.
     *
     * @return an iterable of all commands from root to terminal command
     */
    @NotNull
    Iterable<? extends Command<S>> getCommandsUsed();

    /**
     * Gets all resolved arguments in input order.
     *
     * @return a collection of arguments in the order they appeared in the input
     * @note Flags are not included in this collection
     */
    Collection<? extends ParsedArgument<S>> getParsedArguments();

    /**
     * Checks if a flag parameter was resolved in this context.
     *
     * @param currentParameter the flag parameter to check
     * @return {@code true} if the flag was provided and resolved
     */
    default boolean hasResolvedFlag(Argument<S> currentParameter) {
        if (!currentParameter.isFlag()) {
            return false;
        }
        return hasResolvedFlag(currentParameter.asFlagParameter().flagData());
    }

    /**
     * Checks if a flag was resolved in this context.
     *
     * @param flagData the flag definition to check
     * @return {@code true} if the flag was provided and resolved
     */
    boolean hasResolvedFlag(FlagData<S> flagData);

    /**
     * Debugs the current resolved arguments cached/mapped.<br>
     * This requires {@link ImperatDebugger} to be enabled.<br>
     * Example: <code>ImperatDebugger.setEnabled(true)</code>
     */
    void debug();
}