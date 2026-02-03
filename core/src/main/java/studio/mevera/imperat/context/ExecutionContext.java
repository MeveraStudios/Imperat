package studio.mevera.imperat.context;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.annotations.ContextResolved;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.tree.CommandPathSearch;
import studio.mevera.imperat.context.internal.Argument;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.context.internal.ExtractedFlagArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.resolvers.ContextResolver;
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
 * @see CommandUsage
 * @see Argument
 * @see ExtractedFlagArgument
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
@ContextResolved
public interface ExecutionContext<S extends Source> extends Context<S> {
    
    /**
     * Fetches the command path search instance used to resolve the command execution path.
     * This provides access to the full command hierarchy leading to the executed command.
     * @return the command path search instance
     */
    @NotNull CommandPathSearch<S> getPathwaySearch();
    
    /**
     * Retrieves a flag by its name if it was provided in the command input.
     *
     * @param flagName the name of the flag (without prefix)
     * @return an {@link Optional} containing the flag if present, empty otherwise
     */
    Optional<ExtractedFlagArgument> getFlag(String flagName);
    
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
     * @param name the parameter name defined in the {@link CommandUsage}
     * @return the resolved argument value, or {@code null} if not provided or couldn't be resolved
     * @see Argument
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
        if (argValue != null) return argValue;
        return value;
    }
    
    /**
     * Gets the raw string input for an argument by its position.
     *
     * @param index the zero-based position of the argument in the command input
     * @return the raw argument string, or {@code null} if index is out of bounds
     */
    default String getRawArgument(int index) {
        if (index >= arguments().size() || index < 0) return null;
        return arguments().get(index);
    }
    
    /**
     * Resolves the command source into a different type using the configured source resolver.
     *
     * @param <R> the target type to resolve to
     * @param type the target type class
     * @return the resolved source, never {@code null}
     * @throws CommandException if resolution fails
     * @see ImperatConfig#getSourceResolver(Type) (Type)
     */
    <R> @NotNull R getResolvedSource(Type type) throws CommandException;
    
    /**
     * Gets an argument resolved by the context resolver system.
     *
     * @param <T> the type of the argument
     * @param type the class of the argument type
     * @return the resolved argument value, or {@code null} if not resolvable
     * @throws CommandException if resolution fails
     * @see ContextResolver
     */
    <T> @Nullable T getContextResolvedArgument(Class<T> type) throws CommandException;
    
    /**
     * Gets all flags that were resolved from the command input.
     *
     * @return a collection of resolved flags
     */
    Collection<? extends ExtractedFlagArgument> getResolvedFlags();
    
    /**
     * Resolves and registers a command argument.
     *
     * @param <T> the type of the argument value
     * @param command the owning command
     * @param raw the raw input string
     * @param index the argument position
     * @param parameter the parameter definition
     * @param value the resolved value
     * @throws CommandException if resolution fails
     */
    <T> void resolveArgument(
            Command<S> command,
            @Nullable String raw,
            int index,
            CommandParameter<S> parameter,
            @Nullable T value
    ) throws CommandException;
    
    /**
     * Resolves and registers a command argument.
     *
     * @param <T> the type of the argument value
     * @param stream the stream having the current input's data.
     * @param value the resolved value.
     * @throws CommandException if resolution fails.
     */
    default <T> void resolveArgument(
            CommandInputStream<S> stream,
            @Nullable T value
    ) throws CommandException {
        resolveArgument(
            getLastUsedCommand(),
            stream.currentRawIfPresent(),
            stream.currentRawPosition(),
            stream.currentParameterIfPresent(),
            value
        );
    }
    
    
    /**
     * Resolves a command flag from its raw components.
     *
     * @param flagDetected the flag parameter definition
     * @param flagRaw the raw flag string (including prefix)
     * @param flagInputRaw the raw flag value (may be null for switches)
     * @param flagInputValue the resolved flag value
     */
    default void resolveFlag(
            FlagData<S> flagDetected,
            String flagRaw,
            @Nullable String flagInputRaw,
            @Nullable Object flagInputValue
    ) {
        resolveFlag(
                new ExtractedFlagArgument(flagDetected, flagRaw, flagInputRaw, flagInputValue)
        );
    }
    
    /**
     * Registers a resolved flag in the context.
     *
     * @param flag the resolved flag to register
     */
    void resolveFlag(ExtractedFlagArgument flag);
    
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
    CommandUsage<S> getDetectedUsage();
    
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
    Argument<S> getResolvedArgument(Command<S> command, String name);
    
    /**
     * Gets all resolved arguments for a specific command.
     *
     * @param command the owning command
     * @return a list of resolved arguments in declaration order
     */
    List<Argument<S>> getResolvedArguments(Command<S> command);
    
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
    Collection<? extends Argument<S>> getResolvedArguments();
    
    /**
     * Checks if a flag parameter was resolved in this context.
     *
     * @param currentParameter the flag parameter to check
     * @return {@code true} if the flag was provided and resolved
     */
    default boolean hasResolvedFlag(CommandParameter<S> currentParameter) {
        if(!currentParameter.isFlag()) {
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