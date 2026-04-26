package studio.mevera.imperat.tests.compound_example;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.command.arguments.type.Cursor;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Compound argument type that demonstrates direct cursor usage: consumes
 * exactly two tokens, peeks before committing, and returns a record-like
 * value. Extends {@link ArgumentType} directly because neither the simple
 * (1-token) nor greedy (rest-of-line) layer fits.
 */
public class Point2DArgumentType extends ArgumentType<TestCommandSource, Point2D> {

    @Override
    public Point2D parse(
            @NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument,
            @NotNull Cursor<TestCommandSource> cursor
    ) throws CommandException {
        String xRaw = cursor.nextOrNull();
        String yRaw = cursor.nextOrNull();
        if (xRaw == null || yRaw == null) {
            throw new CommandException("Invalid coordinates, expected format: <x> <y>");
        }
        return new Point2D(Double.parseDouble(xRaw), Double.parseDouble(yRaw));
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<TestCommandSource> argument) {
        return 2;
    }
}
