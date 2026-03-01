package studio.mevera.imperat.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import studio.mevera.imperat.context.ExecutionResult;

import java.util.function.Consumer;

/**
 * Base class for all Imperat command framework tests.
 * Provides common infrastructure and assertion utilities.
 */
public abstract class BaseImperatTest {

    protected static final TestImperat IMPERAT = ImperatTestGlobals.IMPERAT;
    protected static final TestSource SOURCE = ImperatTestGlobals.GLOBAL_TEST_SOURCE;

    static {
        System.out.println("Loading BaseImperatTest...");
    }

    @BeforeEach
    void setUp() {
        ImperatTestGlobals.resetTestState();
    }

    /**
     * Executes a full command line and returns the result for assertion.
     * Uses the correct execute method that takes the full command line.
     */
    protected ExecutionResult<TestSource> execute(String commandLine) {
        return IMPERAT.execute(SOURCE, commandLine);
    }

    protected ExecutionResult<TestSource> execute(Consumer<TestSource> sourceModifier, String commandLine) {
        TestSource source = new TestSource(SOURCE.origin());
        sourceModifier.accept(source);
        return IMPERAT.execute(source, commandLine);
    }

    /**
     * Asserts that command execution was successful.
     */
    protected void assertSuccess(ExecutionResult<TestSource> result) {
        if (result.hasFailed()) {
            assertNotNull(result.getError());
            result.getError().printStackTrace();
        }
        assertFalse(result.hasFailed(),
                "Expected successful execution but got failure: " + (result.getError() != null ? result.getError().getMessage() : "Unknown error"));
        assertNotNull(result.getExecutionContext(), "Execution context should not be null for successful execution");
    }

    /**
     * Asserts that command execution failed.
     */
    protected void assertFailure(ExecutionResult<TestSource> result) {
        assertTrue(result.hasFailed(), "Expected failed execution but got success");
    }

    /**
     * Asserts that command execution failed with specific error.
     */
    protected void assertFailure(ExecutionResult<TestSource> result, Class<? extends Throwable> expectedError) {
        assertFailure(result);
        if (result.getError() != null) {
            assertInstanceOf(expectedError, result.getError(),
                    "Expected error of type " + expectedError.getSimpleName());
        }
    }

    /**
     * Asserts argument value in successful execution.
     */
    protected <T> void assertArgument(ExecutionResult<TestSource> result, String paramName, T expectedValue) {
        assertSuccess(result);
        T actualValue = result.getExecutionContext().getArgument(paramName);
        assertEquals(expectedValue, actualValue,
                "Argument '" + paramName + "' has unexpected value");
    }

    protected <T> void assertArrayArgs(ExecutionResult<TestSource> result, String paramName, T[] expected) {
        assertSuccess(result);
        T[] actualValue = result.getExecutionContext().getArgument(paramName);
        assertArrayEquals(expected, actualValue, "Argument '" + paramName + "' has unexpected value");
    }

    /**
     * Asserts flag value in successful execution.
     */
    protected <T> void assertFlag(ExecutionResult<TestSource> result, String flagName, T expectedValue) {
        assertSuccess(result);
        T actualValue = result.getExecutionContext().getFlagValue(flagName);
        assertEquals(expectedValue, actualValue,
                "Flag '" + flagName + "' has unexpected value");
    }
}