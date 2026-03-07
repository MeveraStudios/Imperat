package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
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
    public @Nullable Group parse(
            @NotNull ExecutionContext<TestCommandSource> context,
            @NotNull Cursor<TestCommandSource> cursor,
            @NotNull String correspondingInput) throws CommandException {
        String raw = cursor.currentRaw().orElse(null);
        if (raw == null) {
            return null;
        }
        return GroupRegistry.getInstance().getData(raw)
                       .orElseThrow(() -> new CommandException("Unknown group '%s'", raw));
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<TestCommandSource> context, Argument<TestCommandSource> parameter) {
        String raw = context.arguments().getOr(rawPosition, null);
        if (raw == null) {
            return false;
        }
        return GroupRegistry.getInstance().getData(raw).isPresent();
    }

    @Override
    public SuggestionProvider<TestCommandSource> getSuggestionProvider() {
        return suggestionResolver;
    }

}