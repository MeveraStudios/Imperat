package studio.mevera.imperat.tests.compound_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

public class Point2DArgumentType extends ArgumentType<TestCommandSource, Point2D> {


    @Override
    public Point2D parse(@NotNull CommandContext<TestCommandSource> context, @NotNull Argument<TestCommandSource> argument, @NotNull String input)
            throws CommandException {
        String[] split = input.split(" ");
        if (split.length != 2) {
            throw new CommandException("Invalid coordinates, expected format: <x> <y>");
        }
        String xInput = split[0];
        String yInput = split[1];

        return new Point2D(
                Double.parseDouble(xInput), Double.parseDouble(yInput)
        );
    }

    @Override
    public Point2D parse(
            @NotNull ExecutionContext<TestCommandSource> context,
            @NotNull Cursor<TestCommandSource> cursor
    ) throws CommandException {
        //example input: '/teleport 10 20'
        // the format for this arg is '<x> <y>'
        // correspondingInput is the expected raw to be the '10' in the example raw;
        String xInput = cursor.readInput();
        String yInput = cursor.nextInput();

        if (yInput == null) {
            throw new CommandException("Invalid coordinates, expected format: <x> <y>");
        }

        return new Point2D(
                Double.parseDouble(xInput), Double.parseDouble(yInput)
        );
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<TestCommandSource> argument) {
        return 2;
    }
}
