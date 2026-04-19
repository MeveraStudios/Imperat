package studio.mevera.imperat.command.tree;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.CommandPathway;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.permissions.PermissionHolder;

import java.util.List;

/**
 * Represents the result of a direct tree execution.
 * This replaces the old {@code CommandPathSearch} by combining
 * the search and execution into a single step.
 *
 * @param <S> the source type
 * @author Mqzen
 */
public final class TreeExecutionResult<S extends CommandSource> {

    private final @NotNull Status status;
    private final @Nullable ExecutionContext<S> executionContext;
    private final @Nullable CommandPathway<S> matchedPathway;
    private final @NotNull CommandPathway<S> closestUsage;
    private final @Nullable PermissionHolder deniedPermissionHolder;
    private final @NotNull Command<S> lastCommand;
    private final @NotNull List<ParseResult<S>> parsedArguments;
    private final int furthestMatchDepth;

    private TreeExecutionResult(
            @NotNull Status status,
            @Nullable ExecutionContext<S> executionContext,
            @NotNull CommandPathway<S> closestUsage,
            @Nullable CommandPathway<S> matchedPathway,
            @Nullable PermissionHolder deniedPermissionHolder,
            @NotNull Command<S> lastCommand,
            @NotNull List<ParseResult<S>> parsedArguments,
            int furthestMatchDepth
    ) {
        this.status = status;
        this.executionContext = executionContext;
        this.closestUsage = closestUsage;
        this.matchedPathway = matchedPathway;
        this.deniedPermissionHolder = deniedPermissionHolder;
        this.lastCommand = lastCommand;
        this.parsedArguments = parsedArguments;
        this.furthestMatchDepth = furthestMatchDepth;
    }

    /**
     * Creates a successful execution result.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> success(
            @NotNull ExecutionContext<S> executionContext,
            @NotNull CommandPathway<S> closestUsage,
            @NotNull CommandPathway<S> matchedPathway,
            @NotNull Command<S> lastCommand,
            @NotNull List<ParseResult<S>> parsedArguments,
            int furthestMatchDepth
    ) {
        return new TreeExecutionResult<>(Status.SUCCESS, executionContext, closestUsage, matchedPathway, null, lastCommand,
                List.copyOf(parsedArguments), furthestMatchDepth);
    }

    /**
     * Creates a result indicating permission was denied.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> permissionDenied(
            @Nullable CommandPathway<S> closestUsage,
            @Nullable PermissionHolder deniedPermissionHolder,
            @NotNull Command<S> lastCommand,
            int furthestMatchDepth
    ) {
        return new TreeExecutionResult<>(Status.PERMISSION_DENIED, null, closestUsage == null ? lastCommand.getDefaultPathway() : closestUsage,
                closestUsage,
                deniedPermissionHolder,
                lastCommand,
                List.of(),
                furthestMatchDepth);
    }

    /**
     * Creates a result indicating no matching pathway was found.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> noMatch(
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand,
            int furthestMatchDepth
    ) {
        return new TreeExecutionResult<>(Status.NO_MATCH, null, closestUsage == null ? lastCommand.getDefaultPathway() : closestUsage, closestUsage,
                null, lastCommand, List.of(), furthestMatchDepth);
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

    public @NotNull CommandPathway<S> getClosestUsage() {
        return closestUsage;
    }

    public @Nullable PermissionHolder getDeniedPermissionHolder() {
        return deniedPermissionHolder;
    }

    public @NotNull Command<S> getLastCommand() {
        return lastCommand;
    }

    public @NotNull List<ParseResult<S>> getParsedArguments() {
        return parsedArguments;
    }

    public int getFurthestMatchDepth() {
        return furthestMatchDepth;
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
