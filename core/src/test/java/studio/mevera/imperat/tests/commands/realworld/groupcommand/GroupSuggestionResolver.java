package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.context.SuggestionContext;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.tests.TestSource;

import java.util.List;

public class GroupSuggestionResolver implements SuggestionResolver<TestSource> {


    @Override
    public List<String> autoComplete(
            SuggestionContext<TestSource> context,
            Argument<TestSource> parameter
    ) {
        return GroupRegistry.getInstance().getAll()
                       .stream().map(Group::name)
                       .toList();
    }

}