package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.SimpleArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Test fixture: a fixed three-token argument type that consumes
 * {@code <x> <y> <z>}. Built on the multi-arity {@link SimpleArgumentType}
 * constructor so the inner-arity drain path inside
 * {@code CompletableFutureArgument} is exercised end-to-end.
 */
public final class TriCoordArgumentType extends SimpleArgumentType<TestCommandSource, TriCoord> {

    public TriCoordArgumentType() {
        super(3); // exactly three raw tokens
    }

    @Override
    public TriCoord parse(@NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument,
            @NotNull String input) throws CommandException {
        String[] parts = input.split(" ");
        if (parts.length != 3) {
            throw new CommandException("Expected '<x> <y> <z>', got '" + input + "'");
        }
        try {
            return new TriCoord(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
            );
        } catch (NumberFormatException ex) {
            throw new CommandException("Coordinates must be numbers: '" + input + "'");
        }
    }
}
