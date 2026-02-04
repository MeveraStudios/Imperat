package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.tree.CommandPathSearch;

/**
 * Represents the result of a command execution operation in the Imperat framework.
 *
 * <p>This class follows a Result/Either pattern, encapsulating either a successful
 * command execution with its associated context and search results, or a failure
 * state with error information. This design allows for safe error handling without
 * throwing exceptions during command processing.</p>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Creating a successful result
 * ExecutionResult<MySource> success = ExecutionResult.of(context, search);
 *
 * // Creating a failure result with specific error
 * ExecutionResult<MySource> failure = ExecutionResult.failure(new IllegalArgumentException("Invalid command"));
 *
 * // Creating a generic failure result
 * ExecutionResult<MySource> genericFailure = ExecutionResult.failure();
 *
 * // Checking and handling results
 * if (result.hasFailed()) {
 *     Throwable error = result.getError(); // May be null for generic failures
 *     // Handle error case
 * } else {
 *     ExecutionContext<MySource> context = result.getContext();
 *     CommandPathSearch<MySource> search = result.getSearch();
 *     // Process successful execution
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is immutable and thread-safe once constructed.</p>
 *
 * @param <S> the type of command source that extends {@link Source}, representing
 *           the entity that initiated the command execution (e.g., Player, Console)
 *
 * @since 1.0.0
 * @author Imperat Framework
 * @see ExecutionContext
 * @see CommandPathSearch
 * @see Source
 */
public final class ExecutionResult<S extends Source> {

    private final Context<S> context;
    /**
     * The error that occurred during command execution, if any.
     * This field is null for successful executions and non-null for failures.
     */
    private @Nullable Throwable error;
    /**
     * The execution context containing command arguments, source information,
     * and other execution-related data. This field is null for failed executions.
     */
    private ExecutionContext<S> executionContext;
    /**
     * The command path search result containing information about the matched
     * command path and any parsing results. This field is null for failed executions.
     */
    private CommandPathSearch<S> search;

    /**
     * Private constructor for creating a successful execution result.
     *
     * <p>This constructor is used internally by the {@link #of(ExecutionContext, CommandPathSearch, Context)}
     * factory method to create instances representing successful command executions.</p>
     *
     * @param executionContext the execution context containing command execution data
     * @param search the command path search result containing parsing information
     *
     * @throws IllegalArgumentException if either context or search is null
     */
    private ExecutionResult(@NotNull ExecutionContext<S> executionContext, Context<S> context, @NotNull CommandPathSearch<S> search) {
        this.executionContext = executionContext;
        this.context = context;
        this.search = search;
    }

    /**
     * Private constructor for creating a failed execution result.
     *
     * <p>This constructor is used internally by the {@link #failure(Throwable, Context)}
     * factory method to create instances representing failed command executions.</p>
     *
     * @param ex the exception or error that caused the execution failure,
     *           may be null to represent a generic failure without specific error details
     */
    private ExecutionResult(@Nullable Throwable ex, Context<S> context) {
        this.error = ex;
        this.context = context;
    }

    /**
     * Creates a successful execution result with the provided context and search results.
     *
     * <p>This factory method should be used when a command has been successfully
     * parsed and executed without errors. The resulting {@code ExecutionResult}
     * will contain the execution context and command path search information.</p>
     *
     * @param <S> the type of command source
     * @param context the execution context containing command execution data
     * @param search the command path search result containing parsing and matching information
     *
     * @return a new {@code ExecutionResult} instance representing successful execution
     *
     * @throws IllegalArgumentException if either context or search is null
     *
     * @see #hasFailed()
     * @see #getExecutionContext()
     * @see #getSearch()
     */
    public static <S extends Source> ExecutionResult<S> of(
            ExecutionContext<S> executionContext,
            CommandPathSearch<S> search,
            Context<S> context
    ) {
        return new ExecutionResult<>(executionContext, context, search);
    }

    /**
     * Creates a failed execution result with the provided error information.
     *
     * <p>This factory method should be used when a command execution has failed
     * due to parsing errors, validation failures, runtime exceptions, or other
     * error conditions. The resulting {@code ExecutionResult} will contain only
     * the error information, with context and search fields being null.</p>
     *
     * @param <S> the type of command source
     * @param error the exception or error that caused the execution failure,
     *              may be null to represent a generic failure without specific error details
     *
     * @return a new {@code ExecutionResult} instance representing failed execution
     *
     * @see #hasFailed()
     * @see #getError()
     * @see #failure(Context)
     */
    public static <S extends Source> ExecutionResult<S> failure(@Nullable Throwable error, Context<S> context) {
        return new ExecutionResult<>(error, context);
    }

    /**
     * Creates a failed execution result without specific error information.
     *
     * <p>This is a convenience factory method that creates a generic failure result
     * when no specific error details are available or needed. This is equivalent
     * to calling {@code failure(null)} but provides cleaner, more readable code
     * for cases where the failure is expected or the specific error is not relevant.</p>
     *
     * <p><strong>Use Cases:</strong></p>
     * <ul>
     *   <li>Command validation failures where the error message is handled elsewhere</li>
     *   <li>Permission checks that simply succeed or fail without detailed error info</li>
     *   <li>Generic failure states where the context provides sufficient information</li>
     * </ul>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Instead of writing:
     * return ExecutionResult.failure(null);
     *
     * // You can write:
     * return ExecutionResult.failure();
     *
     * // Usage in conditional logic:
     * if (!hasPermission(source)) {
     *     return ExecutionResult.failure(); // Clean, readable failure
     * }
     * }</pre>
     *
     * @param <S> the type of command source
     *
     * @return a new {@code ExecutionResult} instance representing a generic failed execution
     *
     * @see #failure(Throwable, Context)
     * @see #hasFailed()
     */
    public static <S extends Source> ExecutionResult<S> failure(Context<S> context) {
        return failure(null, context);
    }

    /**
     * Determines whether this execution result represents a failed command execution.
     *
     * <p>An execution is considered failed if either the execution context or
     * command path search is null, which occurs when the result was created
     * using the {@link #failure(Throwable, Context)} factory method.</p>
     *
     * <p><strong>Note:</strong> A failed result may or may not have associated
     * error information accessible via {@link #getError()}.</p>
     *
     * @return {@code true} if the command execution failed, {@code false} if it succeeded
     *
     * @see #failure(Throwable, Context)
     * @see #getError()
     */
    public boolean hasFailed() {
        return executionContext == null || search == null;
    }

    /**
     * Returns the error that caused the command execution to fail, if any.
     *
     * <p>This method returns the exception or error information associated with
     * a failed execution. For successful executions (where {@link #hasFailed()}
     * returns {@code false}), this method will return {@code null}.</p>
     *
     * <p><strong>Note:</strong> Even for failed executions, the error may be null
     * if the failure was created without specific error details.</p>
     *
     * @return the error that caused execution failure, or {@code null} if no
     *         specific error information is available or if the execution succeeded
     *
     * @see #hasFailed()
     * @see #failure(Throwable, Context)
     */
    public @Nullable Throwable getError() {
        return error;
    }

    public Context<S> getContext() {
        return context;
    }

    /**
     * Returns the command path search result containing parsing and matching information.
     *
     * <p>This method provides access to the {@link CommandPathSearch} instance that
     * contains information about how the command was parsed, which command path was
     * matched, and any argument parsing results.</p>
     *
     * <p><strong>Important:</strong> This method should only be called on successful
     * execution results. For failed executions (where {@link #hasFailed()} returns
     * {@code true}), this method will return {@code null}.</p>
     *
     * @return the command path search result for successful executions,
     *         or {@code null} for failed executions
     *
     * @throws IllegalStateException if called on a failed execution result
     *                              (optional runtime check, implementation-dependent)
     *
     * @see #hasFailed()
     * @see CommandPathSearch
     */
    public CommandPathSearch<S> getSearch() {
        return search;
    }

    /**
     * Returns the execution context containing command execution data and environment.
     *
     * <p>This method provides access to the {@link ExecutionContext} instance that
     * contains the command source, parsed arguments, execution environment, and
     * other contextual information needed for command processing.</p>
     *
     * <p><strong>Important:</strong> This method should only be called on successful
     * execution results. For failed executions (where {@link #hasFailed()} returns
     * {@code true}), this method will return {@code null}.</p>
     *
     * @return the execution context for successful executions,
     *         or {@code null} for failed executions
     *
     * @throws IllegalStateException if called on a failed execution result
     *                              (optional runtime check, implementation-dependent)
     *
     * @see #hasFailed()
     * @see ExecutionContext
     */
    public ExecutionContext<S> getExecutionContext() {
        return executionContext;
    }
}