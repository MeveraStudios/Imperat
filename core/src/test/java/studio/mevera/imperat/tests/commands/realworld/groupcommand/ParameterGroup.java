package studio.mevera.imperat.tests.commands.realworld.groupcommand;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.CommandParameter;
import studio.mevera.imperat.command.parameters.type.BaseParameterType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.CommandInputStream;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.exception.SourceException;
import studio.mevera.imperat.resolvers.SuggestionResolver;
import studio.mevera.imperat.tests.TestSource;

public final class ParameterGroup extends BaseParameterType<TestSource, Group> {
    private final GroupSuggestionResolver suggestionResolver = new GroupSuggestionResolver();

    public ParameterGroup() {
        super();
        //static plain suggestions
    }

    @Override
    public @Nullable Group resolve(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull CommandInputStream<TestSource> commandInputStream,
            @NotNull String input) throws CommandException {
        String raw = commandInputStream.currentRaw().orElse(null);
        if (raw == null) {
            return null;
        }
        return GroupRegistry.getInstance().getData(raw)
            .orElseThrow(() -> new SourceException("Unknown group '%s'", raw));
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, CommandParameter<TestSource> parameter) {
        String raw =context.arguments().getOr(rawPosition, null);
        if (raw == null) {
            return false;
        }
        return GroupRegistry.getInstance().getData(raw).isPresent();
    }

    @Override
    public SuggestionResolver<TestSource> getSuggestionResolver() {
        return suggestionResolver;
    }

}