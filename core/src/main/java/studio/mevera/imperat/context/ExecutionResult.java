package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the result of a command execution operation in the Imperat framework.
 *
 * <p>This class follows a Result/Either pattern, encapsulating either a successful
 * command execution with its associated context, or a failure state with error information.</p>
 *
 * <p><strong>Thread Safety:</strong> This class is immutable and thread-safe once constructed.</p>
 *
 * @param <S> the type of command source that extends {@link Source}
 * @since 1.0.0
 * @see ExecutionContext
 * @see Source
 */
public final class ExecutionResult<S extends Source> {

    private final CommandContext<S> context;
    private @Nullable Throwable error;
    private ExecutionContext<S> executionContext;

    private ExecutionResult(@NotNull ExecutionContext<S> executionContext, CommandContext<S> context) {
        this.executionContext = executionContext;
        this.context = context;
    }

    private ExecutionResult(@Nullable Throwable ex, CommandContext<S> context) {
        this.error = ex;
        this.context = context;
    }

    /**
     * Creates a successful execution result.
     *
     * @param <S>              the type of command source
     * @param executionContext  the execution context containing command execution data
     * @param context          the original context
     * @return a new {@code ExecutionResult} instance representing successful execution
     */
    public static <S extends Source> ExecutionResult<S> of(
            ExecutionContext<S> executionContext,
            CommandContext<S> context
    ) {
        return new ExecutionResult<>(executionContext, context);
    }

    /**
     * Creates a failed execution result with the provided error information.
     *
     * @param <S>   the type of command source
     * @param error the exception that caused the failure
     * @param context the context
     * @return a new {@code ExecutionResult} instance representing failed execution
     */
    public static <S extends Source> ExecutionResult<S> failure(@Nullable Throwable error, CommandContext<S> context) {
        return new ExecutionResult<>(error, context);
    }

    /**
     * Creates a failed execution result without specific error information.
     *
     * @param <S>     the type of command source
     * @param context the context
     * @return a new {@code ExecutionResult} instance representing a generic failed execution
     */
    public static <S extends Source> ExecutionResult<S> failure(CommandContext<S> context) {
        return failure(null, context);
    }

    /**
     * Determines whether this execution result represents a failed command execution.
     *
     * @return {@code true} if the command execution failed, {@code false} if it succeeded
     */
    public boolean hasFailed() {
        return executionContext == null;
    }

    /**
     * Returns the error that caused the command execution to fail, if any.
     *
     * @return the error that caused execution failure, or {@code null}
     */
    public @Nullable Throwable getError() {
        return error;
    }

    public CommandContext<S> getContext() {
        return context;
    }

    /**
     * Returns the execution context containing command execution data and environment.
     *
     * @return the execution context for successful executions,
     *         or {@code null} for failed executions
     */
    public ExecutionContext<S> getExecutionContext() {
        return executionContext;
    }
}