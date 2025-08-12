package studio.mevera.imperat.tests.enhanced;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import studio.mevera.imperat.context.Context;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.exception.ImperatException;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestSource;

import java.util.List;
import java.util.Objects;


/**
 * Enhanced base class using AssertJ for fluent assertions.
 */
public abstract class EnhancedBaseImperatTest {
    
    protected static final TestImperat IMPERAT = ImperatTestGlobals.IMPERAT;
    protected static final TestSource SOURCE = ImperatTestGlobals.GLOBAL_TEST_SOURCE;
    
    @BeforeEach
    void setUp() {
        ImperatTestGlobals.resetTestState();
    }
    
    protected ExecutionResult<TestSource> execute(String commandLine) {
        try {
            return IMPERAT.execute(SOURCE, commandLine);
        }catch (ImperatException ex) {
            return ExecutionResult.failure(ex, (Context<TestSource>) ex.getCtx());
        }
    }
    
    protected List<String> tabComplete(String commandLine) {
        return IMPERAT.autoComplete(SOURCE, commandLine).join();
    }
    
    /**
     * Creates a fluent assertion for ExecutionResult.
     */
    protected ExecutionResultAssert assertThat(ExecutionResult<TestSource> result) {
        return new ExecutionResultAssert(result);
    }
    
    /**
     * Custom AssertJ assertion class for ExecutionResult with fluent API.
     */
    public static class ExecutionResultAssert extends org.assertj.core.api.AbstractAssert<ExecutionResultAssert, ExecutionResult<TestSource>> {
        
        public ExecutionResultAssert(ExecutionResult<TestSource> actual) {
            super(actual, ExecutionResultAssert.class);
        }
        
        public ExecutionResultAssert isSuccessful() {
            isNotNull();
            if (actual.hasFailed()) {
                String errorMsg = actual.getError() != null ? actual.getError().getMessage() : "Unknown error";
                failWithMessage("Expected successful execution but got failure: <%s>", errorMsg);
            }
            return this;
        }
        
        public ExecutionResultAssert hasFailed() {
            isNotNull();
            if (!actual.hasFailed()) {
                failWithMessage("Expected failed execution but got success");
            }
            return this;
        }
        
        public ExecutionResultAssert hasFailedWith(Class<? extends Throwable> expectedErrorType) {
            hasFailed();
            if (actual.getError() != null) {
                Assertions.assertThat(actual.getError()).isInstanceOf(expectedErrorType);
            }
            return this;
        }
        
        public ExecutionResultAssert hasArgument(String paramName, Object expectedValue) {
            isSuccessful();
            Object actualValue = actual.getExecutionContext().getArgument(paramName);
            if (!Objects.equals(expectedValue, actualValue)) {
                failWithMessage("Expected argument '%s' to be '%s' but was '%s'",
                    paramName, expectedValue, actualValue);
            }
            return this;
        }
        
        public ExecutionResultAssert hasFlag(String flagName, Object expectedValue) {
            isSuccessful();
            Object actualValue = actual.getExecutionContext().getFlagValue(flagName);
            if (!java.util.Objects.equals(expectedValue, actualValue)) {
                failWithMessage("Expected flag <%s> to be <%s> but was <%s>", 
                    flagName, expectedValue, actualValue);
            }
            return this;
        }
        
        public ExecutionResultAssert hasNullArgument(String paramName) {
            return hasArgument(paramName, null);
        }
        
        public ExecutionResultAssert hasArgumentSatisfying(String paramName, org.assertj.core.api.ThrowingConsumer<Object> requirements) {
            isSuccessful();
            Object actualValue = actual.getExecutionContext().getArgument(paramName);
            Assertions.assertThat(actualValue).satisfies(requirements);
            return this;
        }
        
        public ExecutionResultAssert hasArgumentOfType(String paramName, Class<?> expectedType) {
            isSuccessful();
            Object actualValue = actual.getExecutionContext().getArgument(paramName);
            Assertions.assertThat(actualValue).isInstanceOf(expectedType);
            return this;
        }
        
        public ExecutionResultAssert satisfies(org.assertj.core.api.ThrowingConsumer<ExecutionResult<TestSource>> requirements) {
            isNotNull();
            Assertions.assertThat(actual).satisfies(requirements);
            return this;
        }
        
        public ExecutionResultAssert satisfiesAll(org.assertj.core.api.ThrowingConsumer<ExecutionResult<TestSource>>... requirements) {
            isNotNull();
            for (org.assertj.core.api.ThrowingConsumer<ExecutionResult<TestSource>> requirement : requirements) {
                Assertions.assertThat(actual).satisfies(requirement);
            }
            return this;
        }
        
        public ExecutionResultAssert hasFlagValue(String flagName, Object expectedValue) {
            return hasFlag(flagName, expectedValue);
        }
        
        public ExecutionResultAssert hasSwitchEnabled(String switchName) {
            return hasFlag(switchName, true);
        }
        
        public ExecutionResultAssert hasSwitchDisabled(String switchName) {
            return hasFlag(switchName, false);
        }
    }
}