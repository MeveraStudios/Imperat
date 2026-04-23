package studio.mevera.imperat.tests.parameters;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.command.arguments.Argument;
import studio.mevera.imperat.command.arguments.type.ArgumentType;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Test {@link ArgumentType} that throws a typed {@link ParseFailedException} when
 * the input is {@code "crash"}. Used to regression-test that typed exceptions from
 * {@code ArgumentType#parse} are propagated to command-specific exception handlers
 * instead of being swallowed into a generic {@code InvalidSyntaxException}.
 */
public final class ThrowingArgumentType extends ArgumentType<TestCommandSource, ThrowingValue> {

    @Override
    public ThrowingValue parse(
            @NotNull CommandContext<TestCommandSource> context,
            @NotNull Argument<TestCommandSource> argument,
            @NotNull String input
    ) throws CommandException {
        if ("crash".equalsIgnoreCase(input)) {
            throw new ParseFailedException("Typed parse failure for '" + input + "'");
        }
        if ("unhandled".equalsIgnoreCase(input)) {
            // Unchecked exception that is NOT a CommandException and has no registered handler —
            // should fall back to InvalidSyntaxException so the user still gets a usable error.
            throw new UnhandledParseRuntimeException("Unhandled parse failure for '" + input + "'");
        }
        return new ThrowingValue(input);
    }

    @Override
    public int getNumberOfParametersToConsume(Argument<TestCommandSource> argument) {
        return 1;
    }
}
