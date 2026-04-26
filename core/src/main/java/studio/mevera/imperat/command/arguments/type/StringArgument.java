package studio.mevera.imperat.command.arguments.type;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.CommandSource;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.util.priority.Priority;

/**
 * String argument that adapts at runtime to its declaring {@link Argument}:
 * an argument annotated {@code @Greedy} consumes the cursor's remaining
 * tokens; otherwise it consumes a single token. Cannot extend
 * {@link SimpleArgumentType} or {@link GreedyArgumentType} because the choice
 * is per-argument-instance, not per-type.
 */
public final class StringArgument<S extends CommandSource> extends ArgumentType<S, String> {
    StringArgument() {
        super();
    }

    @Override
    public String parse(@NotNull CommandContext<S> context, @NotNull Argument<S> argument, @NotNull Cursor<S> cursor) throws CommandException {
        String input = isGreedy(argument) ? cursor.collectRemaining() : cursor.nextOrNull();
        if (input == null || input.isEmpty()) {
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
        return isGreedy(argument) ? -1 : 1;
    }

    @Override
    public @NotNull Priority getPriority() {
        return Priority.LOW;
    }
}
