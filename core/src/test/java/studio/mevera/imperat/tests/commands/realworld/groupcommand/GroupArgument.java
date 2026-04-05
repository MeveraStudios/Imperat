package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;

public final class GroupArgument extends ArgumentType<TestCommandSource, Group> {

    private final GroupSuggestionProvider suggestionResolver = new GroupSuggestionProvider();

    public GroupArgument() {
        super();
        //static plain suggestions
    }

    @Override
    public @NotNull Group parse(
            @NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument, @NotNull String raw
    ) throws CommandException {
        return GroupRegistry.getInstance().getData(raw)
                       .orElseThrow(() -> new CommandException("Unknown group '%s'".formatted(raw)));
    }
    @Override
    public SuggestionProvider<TestCommandSource> getSuggestionProvider() {
        return suggestionResolver;
    }

}