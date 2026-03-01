package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

import java.time.Duration;

public final class JavaDurationArgumentType extends ArgumentType<TestSource, Duration> {

    public JavaDurationArgumentType() {
        super();
    }

    @Override
    public @Nullable Duration parse(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        return JavaDurationParser.parseDuration(correspondingInput);
    }

    @Override
    public boolean matchesInput(int rawPosition, CommandContext<TestSource> context, Argument<TestSource> parameter) {
        String input = context.arguments().get(rawPosition);
        if (input == null) {
            return false;
        }

        try {
            System.out.println("MATCHING DURATION");
            JavaDurationParser.parseDuration(input);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}