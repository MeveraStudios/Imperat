package studio.mevera.imperat.context;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.suggestions.CompletionArg;

/**
 * Represents the context for auto-completion while providing suggestions
 *
 * @param <S> the command-source
 */
public interface SuggestionContext<S extends Source> extends Context<S> {

    /**
     * @return The info about the argument being completed
     */
    @NotNull
    CompletionArg getArgToComplete();


}
