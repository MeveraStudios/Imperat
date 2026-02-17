package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.tests.TestSource;

/**
 * INVALID: Greedy parameter must be the last parameter.
 * This should throw IllegalStateException.
 */
@Command("invalid-greedy-middle")
public class InvalidGreedyInMiddle {

    @Execute
    public void execute(TestSource source, 
                       @Greedy String greedy, 
                       String afterGreedy) {
        source.reply("Greedy: " + greedy + ", After: " + afterGreedy);
    }
}

