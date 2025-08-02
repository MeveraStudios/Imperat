package dev.velix.imperat.context.internal;

import dev.velix.imperat.Imperat;
import dev.velix.imperat.command.Command;
import dev.velix.imperat.command.CommandUsage;
import dev.velix.imperat.command.suggestions.AutoCompleter;
import dev.velix.imperat.command.tree.CommandPathSearch;
import dev.velix.imperat.context.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
final class DefaultContextFactory<S extends Source> extends ContextFactory<S> {


    DefaultContextFactory() {
        super();
    }

    /**
     * @param source  the sender/source of this command execution
     * @param command the command label used
     * @param queue   the args input
     * @return new context from the command and args used by {@link Source}
     */
    @Override
    public @NotNull Context<S> createContext(
        @NotNull Imperat<S> imperat,
        @NotNull S source,
        @NotNull Command<S> command,
        @NotNull String label,
        @NotNull ArgumentInput queue
    ) {
        return new ContextImpl<>(imperat, command, source, label, queue);
    }

    @Override
    public SuggestionContext<S> createSuggestionContext(
        @NotNull Imperat<S> imperat,
        @NotNull S source,
        @NotNull Command<S> command,
        @NotNull String label,
        @NotNull ArgumentInput queue
    ) {
        return new SuggestionContextImpl<>(imperat, command, source, label, queue, AutoCompleter.getLastArg(queue));
    }

    /**
     * @param plainContext the context plain
     * @return the context after resolving args into values for
     * later on parsing it into the execution
     */
    @Override
    public ExecutionContext<S> createExecutionContext(
            @NotNull Context<S> plainContext,
            @NotNull CommandPathSearch<S> dispatch
    ) {
        return new ExecutionContextImpl<>(
            plainContext,
            dispatch
        );
    }
    
    /**
     * @param plainContext the context plain
     * @return the context after resolving args into values for
     * later on parsing it into the execution
     */
    @Override
    public ExecutionContext<S> createExecutionContext(
            @NotNull Context<S> plainContext,
            @NotNull CommandUsage<S> usage
    ) {
        return new ExecutionContextImpl<>(
                plainContext,
                usage
        );
    }
}
