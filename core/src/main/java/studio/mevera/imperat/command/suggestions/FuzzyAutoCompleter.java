package studio.mevera.imperat.command.suggestions;

import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An {@link AutoCompleter} that applies case-insensitive substring matching instead of
 * the native case-sensitive prefix filter. Typing "jojo" will surface "Ein_Jojo".
 *
 * @param <S> the command-source type
 */
public final class FuzzyAutoCompleter<S extends CommandSource> extends AutoCompleter<S> {

    public FuzzyAutoCompleter(Command<S> command) {
        super(command);
    }

    @Override
    public CompletableFuture<List<String>> autoComplete(SuggestionContext<S> context) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> all = command.tree().tabCompleteRaw(context);
            String prefix = context.getArgToComplete().value();
            if (prefix.isBlank()) {
                return all;
            }
            String lower = prefix.toLowerCase();
            return all.stream()
                    .filter(s -> s.toLowerCase().contains(lower))
                    .toList();
        });
    }
}
