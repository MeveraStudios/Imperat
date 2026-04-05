package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

import java.time.Duration;

public final class JavaDurationArgumentType extends ArgumentType<TestCommandSource, Duration> {

    public JavaDurationArgumentType() {
        super();
    }

    @Override
    public Duration parse(@NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input)
            throws CommandException {
        return JavaDurationParser.parseDuration(input);
    }


}