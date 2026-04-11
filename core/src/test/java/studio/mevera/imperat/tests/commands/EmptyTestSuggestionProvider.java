package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.List;

public final class EmptyTestSuggestionProvider implements SuggestionProvider<TestCommandSource> {

    @Override
    public List<String> provide(SuggestionContext<TestCommandSource> context, Argument<TestCommandSource> argument) {
        return List.of();
    }
}
