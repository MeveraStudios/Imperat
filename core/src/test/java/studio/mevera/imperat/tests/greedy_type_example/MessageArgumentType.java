package studio.mevera.imperat.tests.greedy_type_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

public final class MessageArgumentType extends ArgumentType<TestCommandSource, Message> {

    @Override
    public Message parse(@NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input)
            throws CommandException {
        return new Message(String.join(" ", input.split(" ")));
    }

    @Override
    public Message parse(@NotNull ExecutionContext<TestCommandSource> context, @NotNull Cursor<TestCommandSource> cursor) throws CommandException {
        // Greedy: take all remaining input and create a message
        return new Message(cursor.collectRemainingRaw());
    }

    @Override
    public boolean isGreedy(Argument<TestCommandSource> parameter) {
        return true;
    }
}
