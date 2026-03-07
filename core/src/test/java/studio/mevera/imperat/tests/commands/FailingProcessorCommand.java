package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Processor;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.context.CommandContext;
import studio.mevera.imperat.exception.CommandException;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * Test command whose pre-processor always throws a {@link CommandException},
 * which should prevent the command from being executed.
 */
@RootCommand("proctestfail")
public class FailingProcessorCommand {

    @Processor
    public void blockingPreProcessor(CommandContext<TestCommandSource> context) throws CommandException {
        throw new CommandException("Blocked by pre-processor");
    }

    @Execute
    public void defaultUsage(TestCommandSource source) {
        source.reply("proctestfail default — should never run");
    }
}

