package studio.mevera.imperat;


import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.context.Source;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the class that handles all
 * commands' registrations and executions
 * It also caches the settings that the user can
 * change or modify in the api.
 *
 * @param <S> the command sender valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public non-sealed interface Imperat<S extends Source> extends AnnotationInjector<S>, CommandRegistrar<S>, SourceWrapper<S> {


    /**
     * @return the platform of the module
     */
    Object getPlatform();

    /**
     * Shuts down the platform
     */
    void shutdownPlatform();

    /**
     * The config for imperat
     *
     * @return the config holding all variables.
     */
    @NotNull ImperatConfig<S> config();

    /**
     * Dispatches and executes a command using {@link Context} only
     *
     * @param context the context
     * @return the usage match setResult
     */
    @NotNull ExecutionResult<S> execute(@NotNull Context<S> context) throws Throwable;

    /**
     * Dispatches and executes a command with certain raw arguments
     * using {@link Command}
     *
     * @param source   the sender/executor of this command
     * @param command  the command object to execute
     * @param rawInput the command's args input
     * @return the usage match setResult
     */
    @NotNull ExecutionResult<S> execute(
            @NotNull S source, @NotNull Command<S> command,
            @NotNull String commandName, @NotNull String... rawInput);

    /**
     * Dispatches and executes a command with certain raw arguments
     *
     * @param sender      the sender/executor of this command
     * @param commandName the name of the command to execute
     * @param rawInput    the command's args input
     * @return the usage match setResult
     */
    @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String[] rawInput);

    /**
     * Dispatches and executes a command with certain raw arguments
     *
     * @param sender         the sender/executor of this command
     * @param commandName    the name of the command to execute
     * @param rawArgsOneLine the command's args input on ONE LINE
     * @return the usage match setResult
     */
    @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandName, @NotNull String rawArgsOneLine);

    /**
     * Dispatches the full command-line
     *
     * @param sender      the source/sender of the command
     * @param commandLine the command line to dispatch
     * @return the usage match setResult
     */
    @NotNull ExecutionResult<S> execute(@NotNull S sender, @NotNull String commandLine);

    /**
     * @param sender          the sender writing the command
     * @param fullCommandLine the full command line
     * @return the suggestions at the current position
     */
    CompletableFuture<List<String>> autoComplete(@NotNull S sender, @NotNull String fullCommandLine);

    /**
     * Debugs all registered commands and their usages.
     *
     * @param treeVisualizing whether to display them in the form of tree
     */
    void debug(boolean treeVisualizing);

}
