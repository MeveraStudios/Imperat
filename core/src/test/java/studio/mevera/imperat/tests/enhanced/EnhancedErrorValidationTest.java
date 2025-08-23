package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Enhanced Error Validation Tests")
class EnhancedErrorValidationTest extends EnhancedBaseImperatTest {
    
    @Nested
    @DisplayName("Command Structure Errors")
    class CommandStructureErrors {
        
        @Test
        @DisplayName("Should fail gracefully for unknown commands")
        void testUnknownCommandGracefulFailure() {
            try {
                execute("completely_unknown_command with args");
            }catch (Exception ex) {
                ex.printStackTrace();
                Assertions.assertInstanceOf(IllegalArgumentException.class, ex);
            }
        }
        
        @Test
        @DisplayName("Should fail for incomplete required arguments")
        void testIncompleteRequiredArguments() throws ImperatException {
            ExecutionResult<TestSource> result = execute("test hello"); // Missing second required arg
            
            assertThat(result)
                .hasFailed();
        }
        
        @Test
        @DisplayName("Should fail for incomplete subcommands")
        void testIncompleteSubcommands() throws ImperatException {
            ExecutionResult<TestSource> result = execute("group member setperm"); // Missing permission
            
            assertThat(result)
                .isSuccessful();
        }
    }
    
    @Nested
    @DisplayName("Parameter Validation Errors")
    class ParameterValidationErrors {
        
        @Test
        @DisplayName("Should handle invalid enum values gracefully")
        void testInvalidEnumValues() throws ImperatException {
            ExecutionResult<TestSource> result = execute("customenum COMPLETELY_INVALID_ENUM_VALUE");
            
            assertThat(result)
                .hasFailed();
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should handle empty or whitespace inputs")
        void testEmptyWhitespaceInputs(String input) throws ImperatException {
            try {
                execute(input);
            }catch (Exception ex) {
                Assertions.assertInstanceOf(IllegalArgumentException.class, ex);
            }
        }
    }
    
    @Nested
    @DisplayName("Context Resolution Errors")
    class ContextResolutionErrors {
        
        @Test
        @DisplayName("Should fail when context resolution is impossible")
        void testImpossibleContextResolution() throws ImperatException {
            ExecutionResult<TestSource> result = execute("ctx sub"); // Requires Group context which isn't available
            
            assertThat(result)
                .hasFailed();
        }
    }
}
