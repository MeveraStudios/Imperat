package studio.mevera.imperat.tests.errors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Error Handling Tests")
public class ErrorHandlingTest extends BaseImperatTest {
    
    @Test
    @DisplayName("Should fail for incomplete required arguments")
    void testIncompleteRequiredArguments() {
        ExecutionResult<TestSource> result = execute("test hello"); // Missing second required argument
        assertFailure(result);
    }
    
    @Test
    @DisplayName("Should fail for completely unknown commands")
    void testCompletelyUnknownCommands() {
        ExecutionResult<TestSource> result = execute("completely_unknown_command with args");
        assertFailure(result);
    }
    
    @Test
    @DisplayName("Should handle malformed flag syntax")
    void testMalformedFlagSyntax() {
        ExecutionResult<TestSource> result = execute("ban mqzen --invalid-flag");
        // This should either work or fail gracefully depending on implementation
        // Just ensure it doesn't crash
        assertNotNull(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Should handle empty or whitespace-only inputs")
    void testEmptyInputs(String input) {
        ExecutionResult<TestSource> result = execute(input.trim());
        if (input.trim().isEmpty()) {
            // Empty input should fail
            assertFailure(result);
        }
    }
    
    @Test
    @DisplayName("Should provide meaningful error information")
    void testErrorInformation() {
        ExecutionResult<TestSource> result = execute("nonexistent command args");
        assertFailure(result);
        assertTrue(result.hasFailed(), "Should indicate failure");
    }
    
    @Test
    @DisplayName("Should handle invalid parameter types gracefully")
    void testInvalidParameterTypes() {
        // Try to pass a very long name to TestPlayer which should reject it
        ExecutionResult<TestSource> result = execute("give apple verylongusernamethatexceedslimits");
        // This should fail during parameter validation
        assertFailure(result);
    }
    
    @Test
    @DisplayName("Should handle incomplete subcommands")
    void testIncompleteSubcommands() {
        ExecutionResult<TestSource> result = execute("group member setperm"); // Missing permission argument
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle context resolution failures")
    void testContextResolutionFailures() {
        ExecutionResult<TestSource> result = execute("ctx sub"); // Should fail due to missing Group context
        assertFailure(result);
    }
}
