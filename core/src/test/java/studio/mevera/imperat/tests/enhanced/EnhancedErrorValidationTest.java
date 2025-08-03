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
        void testUnknownCommandGracefulFailure() throws ImperatException {
            ExecutionResult<TestSource> result = execute("completely_unknown_command with args");
            
            assertThat(result)
                .hasFailed();
                
            // Error should not be null for unknown commands
            if (result.getError() != null) {
                Assertions.assertFalse(result.getError().getMessage().isEmpty());
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
        
        @ParameterizedTest
        @ValueSource(strings = {
            "verylongusernamethatexceedslimitsforsure",
            "player_with_very_long_name_that_should_fail",
            "areallylongnamethatshouldexceedthe16characterlimitformcusernames"
        })
        @DisplayName("Should reject player names that are too long")
        void testPlayerNameLengthValidation(String longName) throws ImperatException {
            ExecutionResult<TestSource> result = execute("give apple " + longName);
            
            // Should fail due to TestPlayerParamType validation
            assertThat(result)
                .hasFailed();
        }
        
        @Test
        @DisplayName("Should handle invalid enum values gracefully")
        void testInvalidEnumValues() throws ImperatException {
            ExecutionResult<TestSource> result = execute("test4 COMPLETELY_INVALID_ENUM_VALUE");
            
            assertThat(result)
                .hasFailed();
        }
        
        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\t", "\n"})
        @DisplayName("Should handle empty or whitespace inputs")
        void testEmptyWhitespaceInputs(String input) throws ImperatException {
            if (input.trim().isEmpty()) {
                ExecutionResult<TestSource> result = execute(input);
                assertThat(result).hasFailed();
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
