package studio.mevera.imperat.tests.compound_example;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.mevera.imperat.command.parameters.type.ArgumentType;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.context.internal.Cursor;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

public class Point2DArgumentType extends ArgumentType<TestSource, Point2D> {


    @Override
    public @Nullable Point2D parse(
            @NotNull ExecutionContext<TestSource> context,
            @NotNull Cursor<TestSource> cursor,
            @NotNull String correspondingInput
    ) throws CommandException {
        //example input: '/teleport 10 20'
        // the format for this arg is '<x> <y>'
        // correspondingInput is the expected raw to be the '10' in the example raw;
        String xInput = correspondingInput;
        String yInput = cursor.nextInput();

        if (yInput == null) {
            throw new CommandException("Invalid coordinates, expected format: <x> <y>");
        }

        return new Point2D(
                Double.parseDouble(xInput), Double.parseDouble(yInput)
        );
    }

    @Override
    public int getNumberOfParametersToConsume() {
        return 2;
    }
}
