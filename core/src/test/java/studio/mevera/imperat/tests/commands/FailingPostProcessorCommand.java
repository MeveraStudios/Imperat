package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Processor;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.ExecutionContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test command whose post-processor always throws a {@link CommandException}.
 * The command itself should execute, but the post-processor failure should propagate.
 */
@RootCommand("proctestpostfail")
public class FailingPostProcessorCommand {

    private static final List<String> CALL_LOG = Collections.synchronizedList(new ArrayList<>());

    public static List<String> getCallLog() {
        return new ArrayList<>(CALL_LOG);
    }

    public static void clearCallLog() {
        CALL_LOG.clear();
    }

    @Processor
    public void failingPostProcessor(ExecutionContext<TestSource> context) throws CommandException {
        CALL_LOG.add("post:threw");
        throw new CommandException("Post-processor failure");
    }

    @Execute
    public void defaultUsage(TestSource source) {
        CALL_LOG.add("exec:default");
        source.reply("proctestpostfail default");
    }

    @Execute
    public void withArg(TestSource source, @Named("arg") String arg) {
        CALL_LOG.add("exec:arg=" + arg);
        source.reply("proctestpostfail arg=" + arg);
    }
}

