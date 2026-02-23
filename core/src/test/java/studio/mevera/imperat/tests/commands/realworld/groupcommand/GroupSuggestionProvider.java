package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestSource;

import java.util.List;

public class GroupSuggestionProvider implements SuggestionProvider<TestSource> {


    @Override
    public List<String> provide(
            SuggestionContext<TestSource> context,
            Argument<TestSource> parameter
    ) {
        return GroupRegistry.getInstance().getAll()
                       .stream().map(Group::name)
                       .toList();
    }

}