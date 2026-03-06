package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Processor;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.tests.TestSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test command that uses {@code @Processor} annotation to define per-command
 * pre-processors and post-processors.
 *
 * <p>Pre-processor methods take a {@link CommandContext} parameter.</p>
 * <p>Post-processor methods take an {@link ExecutionContext} parameter.</p>
 *
 * <pre>
 *   /proctest                           — root default
 *   /proctest &lt;name&gt;                    — root with arg
 *     sub1                              — subcommand (no own processors, root processors still apply)
 *       &lt;value&gt;                         — sub1 with arg
 * </pre>
 */
@RootCommand("proctest")
public class ProcessorTestCommand {

    /**
     * Tracks the order of pre/post processor and execution calls for assertions
     */
    private static final List<String> CALL_LOG = Collections.synchronizedList(new ArrayList<>());

    public static List<String> getCallLog() {
        return new ArrayList<>(CALL_LOG);
    }

    public static void clearCallLog() {
        CALL_LOG.clear();
    }

    // ── Pre-processor (runs BEFORE argument resolution) ──────────────────

    @Processor
    public void onPreProcess(CommandContext<TestSource> context) {
        CALL_LOG.add("root:pre");
    }

    // ── Post-processor (runs AFTER argument resolution) ──────────────────

    @Processor
    public void onPostProcess(ExecutionContext<TestSource> context) {
        CALL_LOG.add("root:post");
    }

    // ── Execute methods ─────────────────────────────────────────────────

    @Execute
    public void defaultUsage(TestSource source) {
        CALL_LOG.add("root:exec:default");
        source.reply("proctest default");
    }

    @Execute
    public void withName(TestSource source, @Named("name") String name) {
        CALL_LOG.add("root:exec:name=" + name);
        source.reply("proctest name=" + name);
    }

    // ── Subcommand with its own processors ──────────────────────────────

    @SubCommand("sub1")
    public static class Sub1 {


        @Execute
        public void defaultUsage(TestSource source) {
            CALL_LOG.add("sub1:exec:default");
            source.reply("proctest sub1 default");
        }

        @Execute
        public void withValue(TestSource source, @Named("value") String value) {
            CALL_LOG.add("sub1:exec:value=" + value);
            source.reply("proctest sub1 value=" + value);
        }
    }
}

