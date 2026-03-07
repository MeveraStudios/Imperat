package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.List;

public class GroupSuggestionProvider implements SuggestionProvider<TestCommandSource> {


    @Override
    public List<String> provide(
            SuggestionContext<TestCommandSource> context,
            Argument<TestCommandSource> parameter
    ) {
        return GroupRegistry.getInstance().getAll()
                       .stream().map(Group::name)
                       .toList();
    }

}