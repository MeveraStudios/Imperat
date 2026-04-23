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
    private final int matchScore;
    private final @Nullable Throwable parseError;

    private TreeExecutionResult(
            @NotNull Status status,
            @Nullable ExecutionContext<S> executionContext,
            @NotNull CommandPathway<S> closestUsage,
            @Nullable CommandPathway<S> matchedPathway,
            @Nullable PermissionHolder deniedPermissionHolder,
            @NotNull Command<S> lastCommand,
            @NotNull List<ParseResult<S>> parsedArguments,
            int furthestMatchDepth,
            int matchScore,
            @Nullable Throwable parseError
    ) {
        this.status = status;
        this.executionContext = executionContext;
        this.closestUsage = closestUsage;
        this.matchedPathway = matchedPathway;
        this.deniedPermissionHolder = deniedPermissionHolder;
        this.lastCommand = lastCommand;
        this.parsedArguments = parsedArguments;
        this.furthestMatchDepth = furthestMatchDepth;
        this.matchScore = matchScore;
        this.parseError = parseError;
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
            int furthestMatchDepth,
            int matchScore
    ) {
        return new TreeExecutionResult<>(Status.SUCCESS, executionContext, closestUsage, matchedPathway, null, lastCommand,
                List.copyOf(parsedArguments), furthestMatchDepth, matchScore, null);
    }

    /**
     * Creates a result indicating permission was denied.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> permissionDenied(
            @Nullable CommandPathway<S> closestUsage,
            @Nullable PermissionHolder deniedPermissionHolder,
            @NotNull Command<S> lastCommand,
            int furthestMatchDepth,
            int matchScore
    ) {
        return new TreeExecutionResult<>(Status.PERMISSION_DENIED, null, closestUsage == null ? lastCommand.getDefaultPathway() : closestUsage,
                closestUsage,
                deniedPermissionHolder,
                lastCommand,
                List.of(),
                furthestMatchDepth,
                matchScore,
                null);
    }

    /**
     * Creates a result indicating no matching pathway was found.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> noMatch(
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand,
            int furthestMatchDepth,
            int matchScore
    ) {
        return noMatch(closestUsage, lastCommand, furthestMatchDepth, matchScore, null);
    }

    /**
     * Creates a no-match result that carries the underlying parse error raised by an
     * {@link studio.mevera.imperat.command.arguments.type.ArgumentType#parse} call, so the
     * original exception (e.g. a user-thrown {@link studio.mevera.imperat.exception.CommandException}
     * subclass) can be surfaced to the configured exception handler instead of being
     * replaced by a generic {@code InvalidSyntaxException}.
     */
    public static <S extends CommandSource> TreeExecutionResult<S> noMatch(
            @Nullable CommandPathway<S> closestUsage,
            @NotNull Command<S> lastCommand,
            int furthestMatchDepth,
            int matchScore,
            @Nullable Throwable parseError
    ) {
        return new TreeExecutionResult<>(Status.NO_MATCH, null, closestUsage == null ? lastCommand.getDefaultPathway() : closestUsage, closestUsage,
                null, lastCommand, List.of(), furthestMatchDepth, matchScore, parseError);
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

    public int getMatchScore() {
        return matchScore;
    }

    /**
     * The exception thrown by the failing {@code ArgumentType#parse} call that caused this
     * {@link Status#NO_MATCH}, or {@code null} if this result is not tied to a specific parse failure.
     */
    public @Nullable Throwable getParseError() {
        return parseError;
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
