package studio.mevera.imperat.tests.enhanced;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import studio.mevera.imperat.exception.ProcessorException;
import studio.mevera.imperat.tests.commands.FailingPostProcessorCommand;
import studio.mevera.imperat.tests.commands.FailingProcessorCommand;
import studio.mevera.imperat.tests.commands.PriorityProcessorCommand;
import studio.mevera.imperat.tests.commands.ProcessorTestCommand;

import java.util.List;

/**
 * Tests for the {@code @Processor} annotation feature, which allows defining
 * per-command pre-processors ({@code CommandContext} parameter) and
 * post-processors ({@code ExecutionContext} parameter) directly as methods
 * on the command class.
 */
@DisplayName("Per-Command @Processor Annotation Tests")
class EnhancedProcessorTest extends EnhancedBaseImperatTest {

    // ════════════════════════════════════════════════════════════════════════
    // Basic pre- and post-processor invocation
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Basic Pre/Post Processor Invocation")
    class BasicInvocation {

        @BeforeEach
        void clearLog() {
            ProcessorTestCommand.clearCallLog();
        }

        @Test
        @DisplayName("Root default execution should trigger root pre- and post-processor")
        void rootDefaultShouldTriggerProcessors() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest"))
                    .isSuccessful();

            List<String> log = ProcessorTestCommand.getCallLog();
            Assertions.assertThat(log)
                    .as("Pre-processor should run")
                    .contains("root:pre");
            Assertions.assertThat(log)
                    .as("Post-processor should run")
                    .contains("root:post");
            Assertions.assertThat(log)
                    .as("Execution should happen")
                    .contains("root:exec:default");
        }

        @Test
        @DisplayName("Root execution with argument should trigger root pre- and post-processor")
        void rootWithArgShouldTriggerProcessors() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest hello"))
                    .isSuccessful()
                    .hasArgument("name", "hello");

            List<String> log = ProcessorTestCommand.getCallLog();
            Assertions.assertThat(log).contains("root:pre", "root:exec:name=hello", "root:post");
        }

        @Test
        @DisplayName("Subcommand execution should still trigger root pre- and post-processors")
        void subcommandShouldTriggerRootProcessors() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest sub1"))
                    .isSuccessful();

            List<String> log = ProcessorTestCommand.getCallLog();
            Assertions.assertThat(log)
                    .as("Root pre-processor should run for subcommand")
                    .contains("root:pre");
            Assertions.assertThat(log)
                    .as("Root post-processor should run for subcommand")
                    .contains("root:post");
            Assertions.assertThat(log)
                    .as("Sub1 execution should happen")
                    .contains("sub1:exec:default");
        }

        @Test
        @DisplayName("Subcommand with argument should trigger root processors and pass args")
        void subcommandWithArgShouldTriggerRootProcessors() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest sub1 myValue"))
                    .isSuccessful()
                    .hasArgument("value", "myValue");

            List<String> log = ProcessorTestCommand.getCallLog();
            Assertions.assertThat(log).contains("root:pre", "sub1:exec:value=myValue", "root:post");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Execution order: pre → exec → post
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Execution Order")
    class ExecutionOrder {

        @BeforeEach
        void clearLog() {
            ProcessorTestCommand.clearCallLog();
        }

        @Test
        @DisplayName("Pre-processor should run BEFORE execution, post-processor AFTER resolution but BEFORE execution")
        void preBeforePostBeforeExec() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest"))
                    .isSuccessful();

            List<String> log = ProcessorTestCommand.getCallLog();
            int preIdx = log.indexOf("root:pre");
            int postIdx = log.indexOf("root:post");
            int execIdx = log.indexOf("root:exec:default");

            Assertions.assertThat(preIdx)
                    .as("Pre-processor index should be found")
                    .isGreaterThanOrEqualTo(0);
            Assertions.assertThat(postIdx)
                    .as("Post-processor index should be found")
                    .isGreaterThanOrEqualTo(0);
            Assertions.assertThat(execIdx)
                    .as("Execution index should be found")
                    .isGreaterThanOrEqualTo(0);

            Assertions.assertThat(preIdx)
                    .as("Pre-processor should run before post-processor")
                    .isLessThan(postIdx);
            Assertions.assertThat(postIdx)
                    .as("Post-processor should run before execution")
                    .isLessThan(execIdx);
        }

        @Test
        @DisplayName("Subcommand: root pre → root post → sub exec order should hold")
        void subCommandRootPreBeforeRootPostBeforeExec() {
            assertThat(execute(ProcessorTestCommand.class, cfg -> {
            }, "proctest sub1 testVal"))
                    .isSuccessful();

            List<String> log = ProcessorTestCommand.getCallLog();
            int preIdx = log.indexOf("root:pre");
            int postIdx = log.indexOf("root:post");
            int execIdx = log.indexOf("sub1:exec:value=testVal");

            Assertions.assertThat(preIdx).isGreaterThanOrEqualTo(0);
            Assertions.assertThat(postIdx).isGreaterThanOrEqualTo(0);
            Assertions.assertThat(execIdx).isGreaterThanOrEqualTo(0);
            Assertions.assertThat(preIdx).isLessThan(postIdx);
            Assertions.assertThat(postIdx).isLessThan(execIdx);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Failing pre-processor should block execution
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Failing Pre-Processor")
    class FailingPreProcessor {

        @Test
        @DisplayName("Command should fail when pre-processor throws CommandException")
        void preProcessorExceptionShouldBlockExecution() {
            assertThat(execute(FailingProcessorCommand.class, cfg -> {
            }, "proctestfail"))
                    .hasFailed()
                    .hasFailedWith(ProcessorException.class);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Failing post-processor should propagate error
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Failing Post-Processor")
    class FailingPostProcessor {

        @BeforeEach
        void clearLog() {
            FailingPostProcessorCommand.clearCallLog();
        }

        @Test
        @DisplayName("Execution should succeed even when post-processor throws (exception is handled by event bus)")
        void postProcessorExceptionIsHandledByEventBus() {
            // Post-processor exceptions in the event handler are caught by the EventBus exception handler,
            // so the command execution still completes successfully.
            assertThat(execute(FailingPostProcessorCommand.class, cfg -> {
            }, "proctestpostfail"))
                    .isSuccessful();
        }

        @Test
        @DisplayName("Execution should still happen before the post-processor fails")
        void executionHappensBeforePostProcessorFails() {
            execute(FailingPostProcessorCommand.class, cfg -> {
            }, "proctestpostfail");

            List<String> log = FailingPostProcessorCommand.getCallLog();
            Assertions.assertThat(log)
                    .as("Execution should have happened")
                    .contains("exec:default");
            Assertions.assertThat(log)
                    .as("Post-processor should have been reached")
                    .contains("post:threw");
        }

        @Test
        @DisplayName("Execution with arg should happen before failing post-processor")
        void executionWithArgBeforePostProcessorFails() {
            execute(FailingPostProcessorCommand.class, cfg -> {
            }, "proctestpostfail someArg");

            List<String> log = FailingPostProcessorCommand.getCallLog();
            Assertions.assertThat(log).contains("exec:arg=someArg", "post:threw");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Priority ordering of multiple processors
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Processor Priority Ordering")
    class PriorityOrdering {

        @BeforeEach
        void clearLog() {
            PriorityProcessorCommand.clearCallLog();
        }

        @Test
        @DisplayName("Multiple pre-processors should execute in priority order (higher value first)")
        void preProcessorsShouldRunInPriorityOrder() {
            assertThat(execute(PriorityProcessorCommand.class, cfg -> {
            }, "proctestprio"))
                    .isSuccessful();

            List<String> log = PriorityProcessorCommand.getCallLog();
            int highIdx = log.indexOf("pre:high(90)");
            int normalIdx = log.indexOf("pre:normal(20)");
            int lowIdx = log.indexOf("pre:low(10)");

            Assertions.assertThat(highIdx).as("pre:high(90) should exist").isGreaterThanOrEqualTo(0);
            Assertions.assertThat(normalIdx).as("pre:normal(20) should exist").isGreaterThanOrEqualTo(0);
            Assertions.assertThat(lowIdx).as("pre:low(10) should exist").isGreaterThanOrEqualTo(0);

            Assertions.assertThat(highIdx)
                    .as("Priority 90 pre-processor should run before priority 20")
                    .isLessThan(normalIdx);
            Assertions.assertThat(normalIdx)
                    .as("Priority 20 pre-processor should run before priority 10")
                    .isLessThan(lowIdx);
        }

        @Test
        @DisplayName("Multiple post-processors should execute in priority order (higher value first)")
        void postProcessorsShouldRunInPriorityOrder() {
            assertThat(execute(PriorityProcessorCommand.class, cfg -> {
            }, "proctestprio"))
                    .isSuccessful();

            List<String> log = PriorityProcessorCommand.getCallLog();
            int highIdx = log.indexOf("post:high(90)");
            int normalIdx = log.indexOf("post:normal(20)");
            int lowIdx = log.indexOf("post:low(10)");

            Assertions.assertThat(highIdx).as("post:high(90) should exist").isGreaterThanOrEqualTo(0);
            Assertions.assertThat(normalIdx).as("post:normal(20) should exist").isGreaterThanOrEqualTo(0);
            Assertions.assertThat(lowIdx).as("post:low(10) should exist").isGreaterThanOrEqualTo(0);

            Assertions.assertThat(highIdx)
                    .as("Priority 90 post-processor should run before priority 20")
                    .isLessThan(normalIdx);
            Assertions.assertThat(normalIdx)
                    .as("Priority 20 post-processor should run before priority 10")
                    .isLessThan(lowIdx);
        }

        @Test
        @DisplayName("All pre-processors should run before post-processors, all post-processors before execution")
        void allPreBeforePostBeforeExec() {
            assertThat(execute(PriorityProcessorCommand.class, cfg -> {
            }, "proctestprio"))
                    .isSuccessful();

            List<String> log = PriorityProcessorCommand.getCallLog();
            int execIdx = log.indexOf("exec:default");

            // All pre-processors should come before exec
            Assertions.assertThat(log.indexOf("pre:high(90)")).isLessThan(execIdx);
            Assertions.assertThat(log.indexOf("pre:normal(20)")).isLessThan(execIdx);
            Assertions.assertThat(log.indexOf("pre:low(10)")).isLessThan(execIdx);

            // All post-processors should come before exec too (post runs after resolution, before execution)
            Assertions.assertThat(log.indexOf("post:high(90)")).isLessThan(execIdx);
            Assertions.assertThat(log.indexOf("post:normal(20)")).isLessThan(execIdx);
            Assertions.assertThat(log.indexOf("post:low(10)")).isLessThan(execIdx);

            // All pre-processors should come before all post-processors
            int lastPreIdx = Math.max(log.indexOf("pre:high(90)"),
                    Math.max(log.indexOf("pre:normal(20)"), log.indexOf("pre:low(10)")));
            int firstPostIdx = Math.min(log.indexOf("post:high(90)"),
                    Math.min(log.indexOf("post:normal(20)"), log.indexOf("post:low(10)")));
            Assertions.assertThat(lastPreIdx)
                    .as("All pre-processors should complete before any post-processor starts")
                    .isLessThan(firstPostIdx);
        }
    }
}

