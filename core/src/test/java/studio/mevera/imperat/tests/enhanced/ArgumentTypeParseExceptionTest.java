package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.commands.ThrowingParseCommand;
import studio.mevera.imperat.tests.parameters.ParseFailedException;
import studio.mevera.imperat.tests.parameters.UnhandledParseRuntimeException;

/**
 * Regression tests for the bug where a typed {@code CommandException} thrown from
 * {@code ArgumentType#parse} was swallowed by the command-tree traversal and surfaced
 * as a generic {@link InvalidSyntaxException}, which prevented any registered
 * {@code @ExceptionHandler} for the original exception type from matching.
 */
@DisplayName("ArgumentType#parse typed exception propagation")
class ArgumentTypeParseExceptionTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should invoke command-specific @ExceptionHandler for a typed parse failure")
    void shouldInvokeHandlerForTypedParseFailure() {
        // "throwparse crash" — ThrowingArgumentType throws ParseFailedException (extends CommandException).
        // ThrowingParseCommand declares @ExceptionHandler(ParseFailedException.class) — since a handler
        // is registered, the typed exception must surface as-is (not as InvalidSyntaxException).
        ExecutionResult<TestCommandSource> result = execute("throwparse crash");

        assertThat(result).hasFailed();

        Assertions.assertThat(result.getError())
                .as("Typed parse exception with a registered handler must surface as-is, not wrapped in InvalidSyntaxException")
                .isInstanceOf(ParseFailedException.class);

        // And the command's local @ExceptionHandler(ParseFailedException.class) must have fired.
        Assertions.assertThat(ThrowingParseCommand.lastHandledType)
                .as("Command-specific handler should have been invoked for ParseFailedException")
                .isEqualTo(ParseFailedException.class);

        Assertions.assertThat(ThrowingParseCommand.lastHandledMessage)
                .as("Handler should have received the original exception message")
                .isEqualTo("Typed parse failure for 'crash'");
    }

    @Test
    @DisplayName("Should still produce InvalidSyntaxException with no cause when there is no parse error (plain no-match)")
    void shouldFallBackToInvalidSyntaxWhenNoParseError() {
        // Supplying NO args is a structural no-match: the required <value> argument is missing.
        // No ArgumentType#parse is ever called — so there is no parse error to carry forward.
        ExecutionResult<TestCommandSource> result = execute("throwparse");

        assertThat(result).hasFailed();
        Assertions.assertThat(result.getError())
                .isInstanceOf(InvalidSyntaxException.class)
                .hasNoCause();
    }

    @Test
    @DisplayName("Should fall back to InvalidSyntaxException when parse throws an unhandled non-CommandException type")
    void shouldFallBackToInvalidSyntaxWhenNoHandlerRegistered() {
        // "throwparse unhandled" — ThrowingArgumentType throws UnhandledParseRuntimeException
        // (a RuntimeException that is NOT a CommandException and has NO registered handler
        // anywhere). The user should still get a usable InvalidSyntaxException with the
        // original parse error preserved as its cause for diagnostics.
        ExecutionResult<TestCommandSource> result = execute("throwparse unhandled");

        assertThat(result).hasFailed();
        Assertions.assertThat(result.getError())
                .as("With no handler registered for a non-CommandException parse failure, the error must be wrapped in InvalidSyntaxException")
                .isInstanceOf(InvalidSyntaxException.class);
        Assertions.assertThat(result.getError().getCause())
                .as("Original parse exception should be preserved as the cause")
                .isInstanceOf(UnhandledParseRuntimeException.class)
                .hasMessage("Unhandled parse failure for 'unhandled'");
    }
}
