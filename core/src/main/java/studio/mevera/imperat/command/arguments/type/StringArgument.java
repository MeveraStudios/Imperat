package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.priority.Priority;

public final class StringArgument<S extends CommandSource> extends ArgumentType<S, String> {
    StringArgument() {
        super();
    }

    @Override
    public String parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull String input) throws CommandException {
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Input is empty");
        }
        return input;
    }

    @Override
    public boolean isGreedy(Argument<S> parameter) {
        return parameter.isGreedyString();
    }
    @Override
    public int getNumberOfParametersToConsume(Argument<S> argument) {
        return 1;
    }
    @Override
    public @NotNull Priority getPriority() {
        return Priority.LOW;
    }
}
