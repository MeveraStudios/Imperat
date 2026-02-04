package studio.mevera.imperat.resolvers;

import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.context.Source;
import studio.mevera.imperat.context.SuggestionContext;

import java.util.List;

final class StaticSuggestionResolver<S extends Source> implements SuggestionResolver<S> {

    private final List<String> suggestions;

    StaticSuggestionResolver(List<String> suggestions) {
        this.suggestions = suggestions;
    }


    @Override
    public List<String> autoComplete(
            SuggestionContext<S> context,
            CommandParameter<S> parameter
    ) {
        return suggestions;
    }
}
