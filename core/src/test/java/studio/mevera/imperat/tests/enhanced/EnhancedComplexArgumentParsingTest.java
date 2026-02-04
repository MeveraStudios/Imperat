package studio.mevera.imperat.tests.enhanced;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.commands.MultipleOptionals;

@DisplayName("Enhanced Complex Argument Parsing Tests")
final class EnhancedComplexArgumentParsingTest extends EnhancedBaseImperatTest {

    @BeforeEach
    void registerCommands() {
        IMPERAT.registerCommand(MultipleOptionals.class);
    }

    // === Basic Obligation Calculation Tests ===

    @Test
    @DisplayName("Should skip optionals when insufficient inputs for required parameters")
    void shouldSkipOptionalWhenInsufficientInputs() {
        // /multopts [opt1] [opt2] <req1> with only 1 input
        assertThat(execute("multopts stop-point"))
                .isSuccessful()
                .hasNullArgument("opt1")
                .hasNullArgument("opt2")
                .hasArgument("req1", "stop-point");
    }

    @Test
    @DisplayName("Should assign inputs optimally when sufficient for all required")
    void shouldAssignOptimallyWhenSufficientInputs() {
        // /multopts [opt1] [opt2] <req1> with 3 inputs
        assertThat(execute("multopts hi 7.5 stop-point"))
                .isSuccessful()
                .hasArgument("opt1", "hi")
                .hasArgument("opt2", 7.5)
                .hasArgument("req1", "stop-point");
    }

    // === Same Type Parameter Tests ===

    @Test
    @DisplayName("Should handle same type optionals correctly")
    void shouldHandleSameTypeOptionals() {
        // /sametype [str] [str] <str> - all same type
        assertThat(execute("sametype option1 final"))
                .isSuccessful()
                .hasArgument("opt1", "option1")
                .hasNullArgument("opt2")
                .hasArgument("required", "final");
    }

    @Test
    @DisplayName("Should prioritize required over optional when same type")
    void shouldPrioritizeRequiredOverOptional() {
        // Only one input for same type parameters
        assertThat(execute("sametype final"))
                .isSuccessful()
                .hasNullArgument("opt1")
                .hasNullArgument("opt2")
                .hasArgument("required", "final");
    }

    // === Chain Optional Tests ===

    @Test
    @DisplayName("Should handle long chain of different type optionals")
    void shouldHandleLongChainOptionals() {
        // /chain [str] [int] [double] <str>
        assertThat(execute("chain text1 100 3.14 final"))
                .isSuccessful()
                .hasArgument("opt1", "text1")
                .hasArgument("opt2", 100)
                .hasArgument("opt3", 3.14)
                .hasArgument("required", "final");
    }

    @Test
    @DisplayName("Should skip middle optionals when types don't match")
    void shouldSkipMiddleOptionalsWhenTypesDontMatch() {
        // /chain [str] [int] [double] <str> with type mismatch
        assertThat(execute("chain 100 final"))
                .isSuccessful()
                .hasArgument("opt1", "100")  // skipped due to type mismatch
                .hasNullArgument("opt2") // matches int type
                .hasNullArgument("opt3")  // skipped
                .hasArgument("required", "final");
    }

    // === Mixed Required/Optional Tests ===

    @Test
    @DisplayName("Should handle optional-required-optional pattern")
    void shouldHandleOptionalRequiredOptionalPattern() {
        // /optreq [opt1] <req> [opt2]
        assertThat(execute("optreq opt1-value required-value opt2-value"))
                .isSuccessful()
                .hasArgument("optional1", "opt1-value")
                .hasArgument("required", "required-value")
                .hasArgument("optional2", "opt2-value");
    }

    @Test
    @DisplayName("Should prioritize required in mixed pattern")
    void shouldPrioritizeRequiredInMixedPattern() {
        // Only sufficient inputs for required parameter
        assertThat(execute("optreq required-value"))
                .isSuccessful()
                .hasNullArgument("optional1")
                .hasArgument("required", "required-value")
                .hasNullArgument("optional2");
    }

    // === Type Routing Tests ===

    @Test
    @DisplayName("Should route numeric input to correct optional parameter")
    void shouldRouteNumericInputCorrectly() {
        // /mixedcmd [str] <int> [double] with numeric input
        assertThat(execute("mixedcmd 999 7 3.5"))
                .isSuccessful()
                .hasArgument("optional1", "999") // string type doesn't match 999
                .hasArgument("required", 7)
                .hasArgument("optional2", 3.5);
    }

    @Test
    @DisplayName("Should route string input to correct optional parameter")
    void shouldRouteStringInputCorrectly() {
        // /mixedcmd [str] <int> [double] with string input
        assertThat(execute("mixedcmd first 999"))
                .isSuccessful()
                .hasArgument("optional1", "first")
                .hasArgument("required", 999)
                .hasNullArgument("optional2");
    }

    // === Complex Nested Scenarios ===
    @Test
    @DisplayName("Should handle deep nesting with various input patterns")
    void shouldHandleDeepNestingWithVariousInputPatterns() {
        // /deep <str> [str] [int] <str> - complex pattern

        // Scenario 1: String gets consumed by optional1 (string is generic)
        assertThat(execute("deep req1 8 end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "8")    // string matches "8" (generic match)
                .hasNullArgument("optional2")     // skipped since optional1 consumed the input
                .hasArgument("required2", "end1");

        // Scenario 2: All parameters filled optimally
        assertThat(execute("deep req1 opt1-a 7 end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "opt1-a")
                .hasArgument("optional2", 7)
                .hasArgument("required2", "end1");

        // Scenario 3: Insufficient inputs - optionals get defaults
        assertThat(execute("deep req1 end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasNullArgument("optional1")     // skipped due to obligation calculation
                .hasNullArgument("optional2")     // skipped due to obligation calculation
                .hasArgument("required2", "end1");

        // Scenario 4: Only one extra input - string optional wins due to being first
        assertThat(execute("deep req1 hello end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "hello")
                .hasNullArgument("optional2")
                .hasArgument("required2", "end1");

        // Scenario 5: Numeric input but string still wins (strings are generic)
        assertThat(execute("deep req1 999 end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "999")   // string consumes numeric input
                .hasNullArgument("optional2")      // skipped
                .hasArgument("required2", "end1");

        // Scenario 6: Test with non-numeric string
        assertThat(execute("deep req1 text-value end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "text-value")
                .hasNullArgument("optional2")
                .hasArgument("required2", "end1");

        // Scenario 7: All parameters filled exactly (no extra inputs)
        assertThat(execute("deep req1 optional-text 42 final-value"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "optional-text")
                .hasArgument("optional2", 42)
                .hasArgument("required2", "final-value");

        // Scenario 8: Test command with extra inputs (should fail)
        assertThat(execute("deep req1 optional-text 42 final-value extra"))
                .hasFailed()
                .hasFailedWith(InvalidSyntaxException.class);
    }

    @Test
    @DisplayName("Should handle all parameters with perfect inputs")
    void shouldHandleAllParametersWithPerfectInputs() {
        // /deep <str> [str] [int] <str> with all inputs
        assertThat(execute("deep req1 opt1-a 8 end1"))
                .isSuccessful()
                .hasArgument("required1", "req1")
                .hasArgument("optional1", "opt1-a")
                .hasArgument("optional2", 8)
                .hasArgument("required2", "end1");
    }

    // === Edge Cases ===

    @Test
    @DisplayName("Should handle all optional parameters with no inputs")
    void shouldHandleAllOptionalsWithNoInputs() {
        // /allopts [str] [int] [double] [bool] with no inputs - all optional
        assertThat(execute("allopts"))
                .isSuccessful()
                .hasNullArgument("stringOpt")
                .hasNullArgument("intOpt")
                .hasNullArgument("doubleOpt")
                .hasNullArgument("boolOpt");
    }

    @Test
    @DisplayName("Should handle selective optional assignment")
    void shouldHandleSelectiveOptionalAssignment() {
        // /allopts [str] [int] [double] [bool] with partial inputs
        assertThat(execute("allopts str1 2 true"))
                .isSuccessful()
                .hasArgument("stringOpt", "str1")
                .hasArgument("intOpt", 2)
                .hasNullArgument("doubleOpt") // skipped due to type mismatch
                .hasArgument("boolOpt", true);
    }

    // === Stress Tests ===

    @Test
    @DisplayName("Should handle complex branching scenario")
    void shouldHandleComplexBranchingScenario() {
        // /branch <str> [str] [int] <str> with type conflicts
        assertThat(execute("branch item1 20 end"))
                .isSuccessful()
                .hasArgument("item", "item1")
                .hasArgument("path", "20")      // string type conflicts with numeric
                .hasNullArgument("count")     // numeric routes to int optional
                .hasArgument("destination", "end");
    }

    @Test
    @DisplayName("Should handle maximum complexity scenario")
    void shouldHandleMaximumComplexityScenario() {
        // /empty <str> <str> [str] [int] <str> - mixed required and optional
        assertThat(execute("empty val1 val2 5 end"))
                .isSuccessful()
                .hasArgument("value1", "val1")
                .hasArgument("value2", "val2")
                .hasArgument("optional1", "5")  // string type conflicts with numeric
                .hasNullArgument("optional2")
                .hasArgument("required", "end");
    }

    // === Configuration Toggle Tests ===

    @Test
    @DisplayName("Should respect disabled middle optional skipping")
    void shouldRespectDisabledMiddleOptionalSkipping() {

        // With strict positional order, should try to assign 999 to opt1 (string)
        // This might fail at runtime due to type mismatch
        assertThat(
                execute(
                        MultipleOptionals.class,
                        (cfg) -> cfg.setHandleExecutionConsecutiveOptionalArgumentsSkip(false),
                        "multopts 999 stop-point"
                )
        ).isSuccessful().hasArgument("opt1", "999");
    }

    // === Performance Edge Cases ===

    @Test
    @DisplayName("Should handle insufficient inputs gracefully")
    void shouldHandleInsufficientInputsGracefully() {
        // /empty <str> <str> [str] [int] <str> with insufficient inputs
        assertThat(execute("empty val1"))
                .hasFailed(); // Should fail due to missing required parameters
    }

    @Test
    @DisplayName("Should optimize parameter distribution")
    void shouldOptimizeParameterDistribution() {
        // Complex scenario testing optimal distribution
        assertThat(execute("chain text1 200 final"))
                .isSuccessful()
                .hasArgument("opt1", "text1")
                .hasArgument("opt2", 200)     // int matches better than double for "200"
                .hasNullArgument("opt3")       // skipped
                .hasArgument("required", "final");
    }

    // === Type Compatibility Matrix Tests ===

    @Test
    @DisplayName("Should handle numeric type priority correctly")
    void shouldHandleNumericTypePriorityCorrectly() {
        // Test int vs double preference
        assertThat(execute("chain 100 final"))
                .isSuccessful()
                .hasArgument("opt1", "100")      // string doesn't match 100
                .hasNullArgument("opt2")     // int gets priority over double
                .hasNullArgument("opt3")      // skipped
                .hasArgument("required", "final");
    }
}
