package studio.mevera.imperat.tests.greedy_type_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.parameters.Argument;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

public final class MessageArgumentType extends ArgumentType<TestSource, Message> {

    @Override
    public @NotNull Message parse(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        //we do not care about the corresponding input, since this is a greedy argument,
        // we will just take all the remaining input and create a message out of it
        return new Message(cursor.collectRemainingRaw());
    }

    @Override
    public boolean isGreedy(Argument<TestSource> parameter) {
        return true;
    }
}
