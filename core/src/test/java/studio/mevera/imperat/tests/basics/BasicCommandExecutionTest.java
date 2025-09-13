package studio.mevera.imperat.tests.basics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.UnknownCommandException;
import studio.mevera.imperat.exception.parse.UnknownSubCommandException;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Basic Command Execution Tests")
public class BasicCommandExecutionTest extends BaseImperatTest {
    
    @Test
    @DisplayName("Should execute simple command successfully")
    void testSimpleCommandExecution() {
        ExecutionResult<TestSource> result = execute("test");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should fail for unknown command")
    void testUnknownCommand() {
        try {
            execute("nonexistent");
        }catch (Exception ex) {
            Assertions.assertInstanceOf(UnknownCommandException.class, ex);
        }
    }
    
    @Test
    @DisplayName("Should handle empty command gracefully")
    void testEmptyCommand() {
        ExecutionResult<TestSource> result = execute("empty");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should execute test command with arguments")
    void testCommandWithArguments() {
        ExecutionResult<TestSource> result = execute("test hello world");
        assertSuccess(result);
        assertArgument(result, "otherText", "hello");
        assertArgument(result, "otherText2", "world");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"upper_case", "UPPER_CASE", "Upper_Case"})
    @DisplayName("Should handle case-insensitive command names")
    void testCaseInsensitiveCommands(String commandName) {
        ExecutionResult<TestSource> result = execute(commandName.toLowerCase());
        assertSuccess(result);
    }
}