package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.providers.SuggestionProvider;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.arguments.TestPlayer;
import studio.mevera.imperat.util.Priority;

import java.util.List;

public final class TestPlayerParamType extends ArgumentType<TestSource, TestPlayer> {

    @Override
    public TestPlayer parse(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput
    ) throws NotPlayerException {
        // Note: We can't easily call matchesInput here without the raw position and context,
        // so we'll just validate the input directly
        if (correspondingInput.length() > 16) {
            throw new NotPlayerException();
        }
        try {
            Double.parseDouble(correspondingInput);
            throw new NotPlayerException(); // Numbers are not valid player names
        } catch (NumberFormatException e) {
            // Good, it's not a number
        }
        return new TestPlayer(correspondingInput);
    }

    @Override
    public boolean matchesInput(int rawPosition, Context<TestSource> context, Argument<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            Double.parseDouble(input);
            return false;
        } catch (Exception exception) {
            System.out.println("Matching input = '" + input + "'");
            System.out.println("Contains '-' ? " + input.contains("-"));
            return !input.contains("-");
        }
    }

    @Override
    public SuggestionProvider<TestSource> getSuggestionProvider() {
        return (ctx, p) -> {
            return List.of("MQZEN", "MOHAMED");
        };
    }

    @Override public Priority priority() {
        return Priority.LOW;
    }
}