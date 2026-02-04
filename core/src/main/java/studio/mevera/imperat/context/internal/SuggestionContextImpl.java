package studio.mevera.imperat.context.internal;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.Imperat;
import studio.mevera.imperat.command.Command;
import studio.mevera.imperat.command.suggestions.CompletionArg;
import studio.mevera.imperat.context.ArgumentInput;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

final class SuggestionContextImpl<S extends Source> extends ContextImpl<S> implements SuggestionContext<S> {

    private final CompletionArg completionArg;

    SuggestionContextImpl(
            Imperat<S> dispatcher,
            Command<S> command,
            S source,
            String label,
            ArgumentInput args,
            CompletionArg completionArg
    ) {
        super(dispatcher, command, source, label, args);
        this.completionArg = completionArg;
    }

    @Override
    public @NotNull CompletionArg getArgToComplete() {
        return completionArg;
    }

}
