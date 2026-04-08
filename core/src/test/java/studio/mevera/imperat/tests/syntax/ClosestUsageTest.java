package studio.mevera.imperat.tests.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestCommandSource;

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
    @DisplayName("Should suggest descendant usage when all required root arguments are missing")
    void testMissingAllRequiredRootArguments() {
        ExecutionResult<TestCommandSource> result = execute("req");
        assertFailure(result, InvalidSyntaxException.class);

        InvalidSyntaxException ex = (InvalidSyntaxException) result.getError();
        assertNotNull(ex, "InvalidSyntaxException should not be null");
        assertNotNull(ex.getClosestUsage(), "Closest usage should not be null");
        assertEquals("<a> <b> <c> <d>", ex.getClosestUsage().formatted(), "Closest usage should point to the required pathway");
    }

    @Test
    @DisplayName("Should suggest correct usage when first required argument is missing")
    void testMissingFirstRequiredArgument() {
        // RootCommand: /usagetest simple <name> <age>
        // Input: /usagetest simple
        // Expected: Should suggest proper usage

        ExecutionResult<TestCommandSource> result = execute("req hi iam idk");
        assertFailure(result, InvalidSyntaxException.class);

        InvalidSyntaxException ex = (InvalidSyntaxException) result.getError();
        assertNotNull(ex, "InvalidSyntaxException should not be null");

        var closestUsage = ex.getClosestUsage();
        System.out.println("Closest usage: " + closestUsage.formatted());

        // The exception should always carry a closest usage hint
        assertNotNull(closestUsage, "Closest usage should not be null");
        assertTrue(closestUsage.formatted().equals("<a> <b> <c> <d>"), "Closest usage should be valid");
    }


}
