package studio.mevera.imperat.providers;

import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;

final class StaticSuggestionProvider<S extends CommandSource> implements SuggestionProvider<S> {

    private final List<String> suggestions;

    StaticSuggestionProvider(List<String> suggestions) {
        this.suggestions = suggestions;
    }


    @Override
    public List<String> provide(
            SuggestionContext<S> context,
            Argument<S> argument
    ) {
        return suggestions;
    }
}
