package studio.mevera.imperat.tests.greedy_type_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

public final class MessageArgumentType extends ArgumentType<TestCommandSource, Message> {

    @Override
    public Message parse(
            @NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input
    ) throws CommandException {
        System.out.println("Parsing input '" + input + "'");
        return new Message(input);
    }

    @Override
    public boolean isGreedy(Argument<TestCommandSource> parameter) {
        return true;
    }
}
