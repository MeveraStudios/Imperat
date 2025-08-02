package dev.velix.imperat.context.internal;

import dev.velix.imperat.Imperat;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.suggestions.CompletionArg;
import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.context.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a way for defining
 * how the context is created
 *
 * @param <S>
 */
@ApiStatus.AvailableSince("1.0.0")
public abstract class ContextFactory<S extends Source> {

    protected ContextFactory() {
    }


    public static <S extends Source> ContextFactory<S> defaultFactory() {
        return new DefaultContextFactory<>();
    }


    /**
     * @param source  the sender/source of this command execution
     * @param command the command label used
     * @param queue   the args input
     * @return new context from the command and args used by {@link Source}
     */
    @NotNull
    public abstract Context<S> createContext(
        @NotNull Imperat<S> imperat,
        @NotNull S source,
        @NotNull Command<S> command,
        @NotNull String label,
        @NotNull ArgumentInput queue
    );

    /**
     * @param source  the source
     * @param command the command
     * @param queue   the argument input
     * @return new context for auto completions with {@link CompletionArg}
     */
    public abstract SuggestionContext<S> createSuggestionContext(
        @NotNull Imperat<S> imperat,
        @NotNull S source,
        @NotNull Command<S> command,
        @NotNull String label,
        @NotNull ArgumentInput queue
    );

    /**
     * @param plainContext the context plain
     * @param dispatch     the result of a search and dispatch for the proper {@link CommandUsage}
     * @return the context after resolving args into values for
     * later on parsing it into the execution
     */
    public abstract ExecutionContext<S> createExecutionContext(
            @NotNull Context<S> plainContext,
            @NotNull CommandPathSearch<S> dispatch
    );
    
    /**
     * @param plainContext the context plain
     * @param usage     the proper {@link CommandUsage}
     * @return the context after resolving args into values for
     * later on parsing it into the execution
     */
    public abstract ExecutionContext<S> createExecutionContext(
            @NotNull Context<S> plainContext,
            @NotNull CommandUsage<S> usage
    );
}
