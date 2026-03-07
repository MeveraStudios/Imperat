package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.commands.BuyCommand;
import studio.mevera.imperat.tests.commands.ExceptionHandlerTestCmd;
import studio.mevera.imperat.tests.commands.TestCommandException;

@DisplayName("Command-Specific Exception Handler Tests")
class CommandExceptionHandlerTest extends EnhancedBaseImperatTest {

    @Test
    @DisplayName("Should invoke command-specific handler when @Range validation fails")
    void shouldInvokeCommandHandlerOnRangeViolation() {
        // buy <item> <quantity:1-50> — quantity 100 is out of range
        // BuyCommand has @ExceptionHandler(ResponseException.class) which should catch
        // the NUMBER_OUT_OF_RANGE ResponseException thrown by RangeValidator
        ExecutionResult<TestCommandSource> result = execute("buy sword 100");

        // The error is handled by the command-local handler, so execution should not propagate as a failure
        assertThat(result).hasFailed();

        Assertions.assertThat(BuyCommand.lastHandledExceptionMessage)
                .as("BuyCommand's local @ExceptionHandler should have been invoked")
                .isNotNull()
                .startsWith("BUY_HANDLER:");
    }

    @Test
    @DisplayName("Should invoke command-specific handler when command throws custom exception")
    void shouldInvokeCommandHandlerOnCustomException() {
        // errtest <action> — passing "crash" causes a TestCommandException to be thrown
        // ExceptionHandlerTestCmd has @ExceptionHandler(TestCommandException.class)
        ExecutionResult<TestCommandSource> result = execute("errtest crash");

        // The exception is thrown and caught by the command-specific handler
        assertThat(result).hasFailed();

        Assertions.assertThat(ExceptionHandlerTestCmd.lastHandledMessage)
                .as("ExceptionHandlerTestCmd's local handler should have caught the exception")
                .isEqualTo("Something went wrong in errtest");

        Assertions.assertThat(ExceptionHandlerTestCmd.lastHandledType)
                .as("The handler should record the correct exception type")
                .isEqualTo(TestCommandException.class);
    }
}


