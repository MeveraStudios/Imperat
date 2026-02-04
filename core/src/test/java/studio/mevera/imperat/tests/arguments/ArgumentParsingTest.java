package studio.mevera.imperat.tests.arguments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

@DisplayName("Argument Parsing Tests")
public class ArgumentParsingTest extends BaseImperatTest {

    @Test
    @DisplayName("Should parse required string arguments correctly")
    void testRequiredStringArguments() {
        ExecutionResult<TestSource> result = execute("test hello world");
        assertSuccess(result);
        assertArgument(result, "otherText", "hello");
        assertArgument(result, "otherText2", "world");
    }

    @Test
    @DisplayName("Should handle optional arguments with defaults")
    void testOptionalArgumentsWithDefaults() {
        ExecutionResult<TestSource> result = execute("give apple");
        assertSuccess(result);
        assertArgument(result, "item", "apple");
        assertArgument(result, "player", null);
        assertArgument(result, "amount", 1);
    }

    @Test
    @DisplayName("Should parse custom parameter types")
    void testCustomParameterTypes() {
        ExecutionResult<TestSource> result = execute("give apple mqzen 5");
        assertSuccess(result);
        assertArgument(result, "item", "apple");
        assertArgument(result, "player", new TestPlayer("mqzen"));
        assertArgument(result, "amount", 5);
    }

    @ParameterizedTest
    @CsvSource({
            "give apple, apple, null, 1",
            "give apple mqzen, apple, mqzen, 1",
            "give apple 5, apple, null, 5"
    })
    @DisplayName("Should handle various optional argument combinations")
    void testOptionalArgumentCombinations(String commandLine, String expectedItem,
            String expectedPlayer, Integer expectedAmount) {
        ExecutionResult<TestSource> result = execute(commandLine);
        assertSuccess(result);
        assertArgument(result, "item", expectedItem);
        TestPlayer expectedPlayerObj = expectedPlayer.equals("null") ? null : new TestPlayer(expectedPlayer);
        assertArgument(result, "player", expectedPlayerObj);
        assertArgument(result, "amount", expectedAmount);
    }

    @Test
    @DisplayName("Should handle greedy arguments")
    void testGreedyArguments() {
        ExecutionResult<TestSource> result = execute("message target this is a long message");
        assertSuccess(result);
        assertArgument(result, "target", "target");
        assertArgument(result, "message", "this is a long message");
    }

    @Test
    @DisplayName("Should parse array parameters")
    void testArrayParameters() {
        ExecutionResult<TestSource> result = execute("test2 array member mod srmod owner");
        assertSuccess(result);

        String[] expectedArray = {"member", "mod", "srmod", "owner"};
        assertArrayArgs(result, "myArray", expectedArray);
    }

    @Test
    @DisplayName("Should handle collection parameters")
    void testCollectionParameters() {
        ExecutionResult<TestSource> result = execute("test2 collection hello world test");
        assertSuccess(result);
        // The collection should contain the parsed elements
    }

    @Test
    @DisplayName("Should handle map parameters")
    void testMapParameters() {
        ExecutionResult<TestSource> result = execute("test2 map key1,value1 key2,value2");
        assertSuccess(result);
        // The map should contain the parsed key-value pairs
    }

}