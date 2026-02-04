package studio.mevera.imperat.tests.syntax;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.command.CommandUsage;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the accuracy of closest usage retrieval when InvalidSyntaxException is thrown.
 * This tests the CommandPathSearch.getClosestUsage() method to ensure it returns the
 * most appropriate usage suggestion based on the user's incomplete or incorrect input.
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
        // Expected: Should suggest "usagetest simple <name> <age>"

        ExecutionResult<TestSource> result = execute("req hi iam idk");
        assertFailure(result, InvalidSyntaxException.class);

        InvalidSyntaxException ex = (InvalidSyntaxException) result.getError();
        assertNotNull(ex, "InvalidSyntaxException should not be null");

        CommandUsage<?> closestUsage = ex.getExecutionResult().getClosestUsage();
        assertNotNull(closestUsage, "Closest usage should not be null");

        String usageString = CommandUsage.format((String) null, closestUsage);
        System.out.println("Closest usage: " + usageString);

        assertTrue(usageString.contains("a"), "Usage should contain 'a' subcommand");
        assertTrue(usageString.contains("b"), "Usage should contain 'b' parameter");
        assertTrue(usageString.contains("c"), "Usage should contain 'c' parameter");
        assertTrue(usageString.contains("d"), "Usage should contain 'd' parameter");
    }


}
