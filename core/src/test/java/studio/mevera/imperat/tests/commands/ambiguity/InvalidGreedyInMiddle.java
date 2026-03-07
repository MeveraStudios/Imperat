package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * INVALID: Greedy parameter must be the last parameter.
 * This should throw IllegalStateException.
 */
@RootCommand("invalid-greedy-middle")
public class InvalidGreedyInMiddle {

    @Execute
    public void execute(TestCommandSource source,
                       @Greedy String greedy, 
                       String afterGreedy) {
        source.reply("Greedy: " + greedy + ", After: " + afterGreedy);
    }
}

