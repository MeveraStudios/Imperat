package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.Source;

/**
 * Represents the result of a direct tree execution.
 * This replaces the old {@code CommandPathSearch} by combining
 * the search and execution into a single step.
 *
 * @param <S> the source type
 * @author Mqzen
 */
public final class TreeExecutionResult<S extends Source> {

    private final @NotNull Status status;
    private final @Nullable ExecutionContext<S> executionContext;
    private final @Nullable CommandPathway<S> matchedPathway;
    private final @Nullable CommandPathway<S> closestUsage;
    private final @NotNull Command<S> lastCommand;

    private TreeExecutionResult(
            @NotNull Status status,
            @Nullable ExecutionContext<S> executionContext,
            @Nullable CommandPathway<S> matchedPathway,
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand
    ) {
        this.status = status;
        this.executionContext = executionContext;
        this.matchedPathway = matchedPathway;
        this.closestUsage = closestUsage;
        this.lastCommand = lastCommand;
    }

    /**
     * Creates a successful execution result.
     */
    public static <S extends Source> TreeExecutionResult<S> success(
            @NotNull ExecutionContext<S> executionContext,
            @NotNull CommandPathway<S> matchedPathway,
            @NotNull Command<S> lastCommand
    ) {
        return new TreeExecutionResult<>(Status.SUCCESS, executionContext, matchedPathway, matchedPathway, lastCommand);
    }

    /**
     * Creates a result indicating permission was denied.
     */
    public static <S extends Source> TreeExecutionResult<S> permissionDenied(
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand
    ) {
        return new TreeExecutionResult<>(Status.PERMISSION_DENIED, null, null, closestUsage, lastCommand);
    }

    /**
     * Creates a result indicating no matching pathway was found.
     */
    public static <S extends Source> TreeExecutionResult<S> noMatch(
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand
    ) {
        return new TreeExecutionResult<>(Status.NO_MATCH, null, null, closestUsage, lastCommand);
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public @Nullable ExecutionContext<S> getExecutionContext() {
        return executionContext;
    }

    public @Nullable CommandPathway<S> getMatchedPathway() {
        return matchedPathway;
    }

    public @Nullable CommandPathway<S> getClosestUsage() {
        return closestUsage;
    }

    public @NotNull Command<S> getLastCommand() {
        return lastCommand;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * The status of a tree execution.
     */
    public enum Status {
        /**
         * The tree traversal found a matching pathway and successfully executed it.
         */
        SUCCESS,

        /**
         * The source does not have permission to execute the matched pathway.
         */
        PERMISSION_DENIED,

        /**
         * No matching pathway was found in the tree for the given input.
         */
        NO_MATCH
    }
}

