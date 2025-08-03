package studio.mevera.imperat.command.suggestions;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

final class NativeAutoCompleter<S extends Source> extends AutoCompleter<S> {

    NativeAutoCompleter(Command<S> command) {
        super(command);
    }

    /**
     * Autocompletes an argument from the whole position of the
     * argument-raw input
     *
     * @param context the context for suggestions
     * @return the auto-completed results
     */
    @Override
    public CompletableFuture<List<String>> autoComplete(SuggestionContext<S> context) {
        return CompletableFuture.supplyAsync(()-> command.tree().tabComplete(context));
    }

}
