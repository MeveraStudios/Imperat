package studio.mevera.imperat.tests.compound_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

public class Point2DArgumentType extends ArgumentType<TestCommandSource, Point2D> {


    @Override
    public Point2D parse(@NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input)
            throws CommandException {
        // The command tree pre-joins the two raw tokens consumed by this type
        // (see {@link #getNumberOfParametersToConsume(Argument)}) into {@code input}
        // separated by a space, so we just split here.
        String[] split = input.split(" ");
        if (split.length != 2) {
            throw new CommandException("Invalid coordinates, expected format: <x> <y>");
        }
        return new Point2D(
                Double.parseDouble(split[0]), Double.parseDouble(split[1])
        );
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<TestCommandSource> argument) {
        return 2;
    }
}
