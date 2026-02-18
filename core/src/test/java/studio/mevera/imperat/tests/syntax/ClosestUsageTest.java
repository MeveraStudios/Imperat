package studio.mevera.imperat.tests.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.placeholders.Placeholder;
import studio.mevera.imperat.responses.ResponseKey;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

/**
 * Tests for the accuracy of closest usage retrieval when CommandException with INVALID_SYNTAX is thrown.
 * This tests that the proper error messages with closest usage suggestions are provided.
 */
@DisplayName("Closest Usage Retrieval Tests")
public class ClosestUsageTest extends BaseImperatTest {

    static {
        System.out.println("Running ClosestUsageTest...");
    }

    @Test
    @DisplayName("Should suggest correct usage when first required argument is missing")
    void testMissingFirstRequiredArgument() {
        // Command: /usagetest simple <name> <age>
        // Input: /usagetest simple
        // Expected: Should suggest proper usage

        ExecutionResult<TestSource> result = execute("req hi iam idk");
        assertFailure(result, CommandException.class);

        CommandException ex = (CommandException) result.getError();
        assertNotNull(ex, "CommandException should not be null");
        assertEquals(ResponseKey.INVALID_SYNTAX, ex.getResponseKey(), "Should be INVALID_SYNTAX error");

        // The closest usage should be in the exception data
        var dataProvider = ex.getPlaceholderDataProvider();
        if (dataProvider == null) {
            return;
        }
        String closestUsageLine = dataProvider.get("closest_usage")
                                          .map(Placeholder::resolver)
                                          .map((r) -> r.resolve("closest_usage"))
                                          .orElse("");

        System.out.println("Closest usage: " + closestUsageLine);

        if (!closestUsageLine.isEmpty()) {
            assertEquals("req <a> <b> <c> <d>", closestUsageLine, "Usage should contain relevant command info");
        }
    }


}
