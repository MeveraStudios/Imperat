package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.util.TypeUtility;
import studio.mevera.imperat.util.priority.Priority;

import java.util.List;

public final class TestPlayerParamType extends ArgumentType<TestCommandSource, TestPlayer> {

    @Override
    public TestPlayer parse(@NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input)
            throws CommandException {
        if (input.length() > 16 || input.length() < 3 || input.contains("-") || TypeUtility.isNumber(input)) {
            throw new CommandException("Invalid player argument: '%s'", input);
        }
        return new TestPlayer(input);
    }

    @Override
    public SuggestionProvider<TestCommandSource> getSuggestionProvider() {
        return (ctx, p) -> {
            return List.of("MQZEN", "MOHAMED");
        };
    }

    @Override public @NotNull Priority getPriority() {
        return Priority.LOW;
    }

    @Override public int getNumberOfParametersToConsume(Argument<TestCommandSource> argument) {
        return 1;
    }
}