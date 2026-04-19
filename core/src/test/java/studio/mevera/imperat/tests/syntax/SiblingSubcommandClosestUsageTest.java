
package studio.mevera.imperat.tests.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.InvalidSyntaxException;
import studio.mevera.imperat.tests.BaseImperatTest;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Regression test for the tree traversal ranking bug where a failing
 * earlier-parsed sibling subcommand (e.g. {@code help [page]}) would
 * hijack the reported closest usage — or worse, leak a speculative
 * parse error like "Invalid Integer Format 'setcolor'" to the user.
 * <p>
 * The root cause: {@code StandardCommandTree#execute} ranked NO_MATCH
 * results by each root-child's static tree depth (always 1), so the
 * first child tried always won. The fix tracks how many input tokens
 * each branch actually consumed before failing and picks the deepest.
 */
@DisplayName("Sibling subcommand closest-usage regression")
public class SiblingSubcommandClosestUsageTest extends BaseImperatTest {

    @Test
    @DisplayName("Missing required args on later sibling should not report the earlier help [page] usage")
    void missingArgs_reportsSetcolorUsage_notHelp() {
        ExecutionResult<TestCommandSource> result = execute("rankreg setcolor");
        assertFailure(result, InvalidSyntaxException.class);

        InvalidSyntaxException ex = (InvalidSyntaxException) result.getError();
        assertNotNull(ex);
        assertNotNull(ex.getClosestUsage());
        assertEquals("setcolor <rank> <color>", ex.getClosestUsage().formatted(),
                "Closest usage must be the setcolor branch the user was trying to invoke, "
                        + "not the earlier-parsed help branch");
    }

    @Test
    @DisplayName("Partially-typed sibling must not surface a speculative parse error from help [page]")
    void partialArgs_doesNotLeakIntParseError() {
        ExecutionResult<TestCommandSource> result = execute("rankreg setcolor OWNER");
        assertFailure(result);

        Throwable error = result.getError();
        assertNotNull(error);
        assertInstanceOf(InvalidSyntaxException.class, error,
                "Speculative parse of 'setcolor' as int for the help [page] branch "
                        + "must not escape as the user-facing error. Got: "
                        + error.getClass().getSimpleName() + " — " + error.getMessage());

        InvalidSyntaxException ex = (InvalidSyntaxException) error;
        assertEquals("setcolor <rank> <color>", ex.getClosestUsage().formatted(),
                "Closest usage must point to the branch that matched furthest");
    }

    @Test
    @DisplayName("Dual-annotated method should remain invokable from the root pathway")
    void rootOptionalHelpUsage_stillExecutes() {
        ExecutionResult<TestCommandSource> result = execute("rankreg 2");
        assertSuccess(result);
        assertArgument(result, "page", 2);
    }

    @Test
    @DisplayName("Dual-annotated method should also remain invokable through its subcommand")
    void subcommandHelpUsage_stillExecutes() {
        ExecutionResult<TestCommandSource> result = execute("rankreg help 2");
        assertSuccess(result);
        assertArgument(result, "page", 2);
    }
}
