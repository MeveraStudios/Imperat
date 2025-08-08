package studio.mevera.imperat.tests.parameters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Custom Parameter Types Tests")
public class CustomParameterTypesTest extends BaseImperatTest {
    
    @ParameterizedTest
    @EnumSource(CustomEnum.class)
    @DisplayName("Should parse custom enum values correctly")
    void testCustomEnumParsing(CustomEnum expectedEnum) {
        ExecutionResult<TestSource> result = execute("customenum " + expectedEnum.name());
        assertSuccess(result);
        assertArgument(result, "enumHere", expectedEnum);
    }
    
    @Test
    @DisplayName("Should fail for invalid enum values")
    void testInvalidEnumValues() {
        ExecutionResult<TestSource> result = execute("customenum INVALID_VALUE");
        assertFailure(result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"1d", "24h", "30m", "permanent"})
    @DisplayName("Should parse duration values correctly")
    void testDurationParsing(String durationStr) {
        ExecutionResult<TestSource> result = execute("rank addperm mod server.fly -customDuration " + durationStr);
        assertSuccess(result);
        assertArgument(result, "rank", "mod");
        assertArgument(result, "permission", "server.fly");
    }
    
    @Test
    @DisplayName("Should handle CompletableFuture parameter types")
    void testCompletableFutureParameterType() {
        ExecutionResult<TestSource> result = execute("testcf Thor is the best hero");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle Optional parameter types")
    void testOptionalParameterType() {
        ExecutionResult<TestSource> result = execute("testoptional Hulk is always angry");
        assertSuccess(result);
    }
    
    @Test
    @DisplayName("Should handle group parameter type")
    void testGroupParameterType() {
        ExecutionResult<TestSource> result = execute("group member");
        assertSuccess(result);
        // Group parameter should resolve to a Group object
    }
    
    @Test
    @DisplayName("Should handle group subcommands")
    void testGroupSubcommands() {
        ExecutionResult<TestSource> result = execute("group member setperm command.test");
        assertSuccess(result);
        assertArgument(result, "permission", "command.test");
    }
}
