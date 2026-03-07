package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Processor;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.tests.TestCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test command with multiple pre- and post-processors at different priorities
 * to verify correct ordering. Higher priority value → runs first.
 */
@RootCommand("proctestprio")
public class PriorityProcessorCommand {

    private static final List<String> CALL_LOG = Collections.synchronizedList(new ArrayList<>());

    public static List<String> getCallLog() {
        return new ArrayList<>(CALL_LOG);
    }

    public static void clearCallLog() {
        CALL_LOG.clear();
    }

    // Priority 90 — highest → should run FIRST
    @Processor(priority = 90)
    public void preHigh(CommandContext<TestCommandSource> context) {
        CALL_LOG.add("pre:high(90)");
    }

    // Priority 20 (default NORMAL) — should run SECOND
    @Processor
    public void preNormal(CommandContext<TestCommandSource> context) {
        CALL_LOG.add("pre:normal(20)");
    }

    // Priority 10 — lowest → should run THIRD
    @Processor(priority = 10)
    public void preLow(CommandContext<TestCommandSource> context) {
        CALL_LOG.add("pre:low(10)");
    }

    // Priority 90 — highest → should run FIRST
    @Processor(priority = 90)
    public void postHigh(ExecutionContext<TestCommandSource> context) {
        CALL_LOG.add("post:high(90)");
    }

    // Priority 20 (default NORMAL) — should run SECOND
    @Processor
    public void postNormal(ExecutionContext<TestCommandSource> context) {
        CALL_LOG.add("post:normal(20)");
    }

    // Priority 10 — lowest → should run THIRD
    @Processor(priority = 10)
    public void postLow(ExecutionContext<TestCommandSource> context) {
        CALL_LOG.add("post:low(10)");
    }

    @Execute
    public void defaultUsage(TestCommandSource source) {
        CALL_LOG.add("exec:default");
        source.reply("proctestprio default");
    }
}


