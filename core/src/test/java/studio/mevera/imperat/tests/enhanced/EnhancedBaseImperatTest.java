package studio.mevera.imperat.tests.enhanced;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import studio.mevera.imperat.ImperatConfig;
import studio.mevera.imperat.context.ExecutionResult;
import studio.mevera.imperat.context.ParsedArgument;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.ImperatTestGlobals;
import studio.mevera.imperat.tests.TestImperat;
import studio.mevera.imperat.tests.TestImperatConfig;
import studio.mevera.imperat.tests.TestSource;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;


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
        return IMPERAT.execute(SOURCE, commandLine);
    }

    protected ExecutionResult<TestSource> execute(
            Class<?> cmdClass,
            Consumer<ImperatConfig<TestSource>> cfgConsumer,
            String commandLine
    ) {
        TestImperat newImperat = TestImperatConfig.builder()
                                         .applyOnConfig(cfgConsumer)
                                         .build();
        newImperat.registerCommand(cmdClass);
        return newImperat.execute(SOURCE, commandLine);
    }

    protected List<String> tabComplete(String commandLine) {
        return IMPERAT.autoComplete(SOURCE, commandLine).join();
    }

    protected List<String> tabComplete(Class<?> cmd, Consumer<ImperatConfig<TestSource>> cfgConsumer, String commandLine) {
        TestImperat newImperat = TestImperatConfig.builder()
                                         .applyOnConfig(cfgConsumer)
                                         .build();
        newImperat.registerCommand(cmd);
        return newImperat.autoComplete(SOURCE, commandLine).join();
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
            ParsedArgument<?> parsedArgument = null;
            for (var arg : actual.getExecutionContext().getParsedArguments()) {
                if (arg.getOriginalArgument().getName().equals(paramName)) {
                    parsedArgument = arg;
                    break;
                }
            }

            if (parsedArgument == null) {
                failWithMessage("No argument found with name '%s'", paramName);
            } else {

                Object actualValue = actual.getExecutionContext().getArgument(paramName);
                if (!Objects.equals(expectedValue, actualValue)) {
                    failWithMessage("Expected argument '%s' to be '%s' but was '%s'",
                            paramName, expectedValue, actualValue);
                }
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

        public <T> ExecutionResultAssert hasContextArgumentOf(Class<T> type, Predicate<T> predicate) {
            isNotNull();
            isSuccessful();
            try {
                T argument = actual.getExecutionContext().getContextArgument(type);
                if (argument != null && predicate.test(argument)) {
                    return this;
                }
                failWithMessage("Expected context argument of type <%s> to satisfy the predicate but it did not. Actual value: <%s>",
                        type.getSimpleName(), argument);
            } catch (CommandException exception) {
                Assertions.fail(exception);
            }

            return this;
        }
    }
}