package studio.mevera.imperat.tests;

import org.junit.jupiter.api.BeforeEach;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ImperatException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for all Imperat command framework tests.
 * Provides common infrastructure and assertion utilities.
 */
public abstract class BaseImperatTest {
    
    protected static final TestImperat IMPERAT = ImperatTestGlobals.IMPERAT;
    protected static final TestSource SOURCE = ImperatTestGlobals.GLOBAL_TEST_SOURCE;
    
    @BeforeEach
    void setUp() {
        ImperatTestGlobals.resetTestState();
    }
    
    /**
     * Executes a full command line and returns the result for assertion.
     * Uses the correct execute method that takes the full command line.
     */
    protected ExecutionResult<TestSource> execute(String commandLine) {
        try {
            return IMPERAT.execute(SOURCE, commandLine);
        } catch (ImperatException e) {
            return ExecutionResult.failure(e);
        }
    }
    
    /**
     * Asserts that command execution was successful.
     */
    protected void assertSuccess(ExecutionResult<TestSource> result) {
        assertFalse(result.hasFailed(), 
            "Expected successful execution but got failure: " + 
            (result.getError() != null ? result.getError().getMessage() : "Unknown error"));
        assertNotNull(result.getContext(), "Execution context should not be null for successful execution");
        assertNotNull(result.getSearch(), "Command path search should not be null for successful execution");
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
        T actualValue = result.getContext().getArgument(paramName);
        assertEquals(expectedValue, actualValue,
            "Argument '" + paramName + "' has unexpected value");
    }
    
    protected <T> void assertArrayArgs(ExecutionResult<TestSource> result, String paramName, T[] expected) {
        assertSuccess(result);
        T[] actualValue = result.getContext().getArgument(paramName);
        assertArrayEquals(expected, actualValue, "Argument '" + paramName + "' has unexpected value");
    }
    
    /**
     * Asserts flag value in successful execution.
     */
    protected <T> void assertFlag(ExecutionResult<TestSource> result, String flagName, T expectedValue) {
        assertSuccess(result);
        T actualValue = result.getContext().getFlagValue(flagName);
        assertEquals(expectedValue, actualValue, 
            "Flag '" + flagName + "' has unexpected value");
    }
}