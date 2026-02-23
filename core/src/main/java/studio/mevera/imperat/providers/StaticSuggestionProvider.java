package studio.mevera.imperat.providers;

import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;

final class StaticSuggestionProvider<S extends Source> implements SuggestionProvider<S> {

    private final List<String> suggestions;

    StaticSuggestionProvider(List<String> suggestions) {
        this.suggestions = suggestions;
    }


    @Override
    public List<String> provide(
            SuggestionContext<S> context,
            Argument<S> parameter
    ) {
        return suggestions;
    }
}
