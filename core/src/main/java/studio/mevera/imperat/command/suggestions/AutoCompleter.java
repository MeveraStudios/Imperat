package studio.mevera.imperat.command.suggestions;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a class that's responsible for
 * Handling all auto-completion processes during
 * tab-completion per one command, regardless of the sender valueType
 */
@ApiStatus.AvailableSince("1.0.0")
public abstract class AutoCompleter<S extends Source> {
    
    protected final Command<S> command;

    protected AutoCompleter(Command<S> command) {
        this.command = command;
    }

    public static <S extends Source> AutoCompleter<S> createNative(Command<S> command) {
        return new NativeAutoCompleter<>(command);
    }

    public static @NotNull CompletionArg getLastArg(ArgumentInput argumentInput) {
        if (argumentInput.isEmpty()) return CompletionArg.EMPTY;
        return new CompletionArg(
                argumentInput.getLast(),
                argumentInput.size() - 1
        );
    }
    
    /**
     * Autocompletes an argument from the whole position of the
     * argument-raw input
     *
     * @param dispatcher the command dispatcher
     * @param sender     the sender writing the command
     * @param args       the args for raw input
     * @return the auto-completed results
     */
    public final CompletableFuture<List<String>> autoComplete(
        final Imperat<S> dispatcher,
        final S sender,
        final String label,
        final String[] args
    ) {
        return CompletableFuture.supplyAsync(()-> {
            StringBuilder builder = new StringBuilder();
            for(var a : args) {
                builder.append(a)
                        .append(" ");
            }
            if(!builder.isEmpty()) {
                builder.deleteCharAt(builder.length() - 1);
            }
            boolean endWithSpace = builder.charAt(builder.length()-1) == ' ';
            ArgumentInput queue = ArgumentInput.parseAutoCompletion(builder.toString(), endWithSpace);
            
            return dispatcher.config().getContextFactory()
                    .createSuggestionContext(dispatcher, sender, command, label, queue);
        }).thenCompose((context)->
                autoComplete(context).exceptionally((ex) -> {
                    dispatcher.config().handleExecutionThrowable(ex, context, AutoCompleter.class, "autoComplete(dispatcher, sender, args)");
                    return Collections.emptyList();
                })
        );
    }

    /**
     * Autocompletes an argument from the whole position of the
     * argument-raw input
     *
     * @param context the context for suggestions
     * @return the auto-completed results
     */
    public abstract CompletableFuture<List<String>> autoComplete(
        SuggestionContext<S> context
    );
}
