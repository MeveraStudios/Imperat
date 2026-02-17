package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.tests.TestSource;

/**
 * INVALID: Greedy parameter with another parameter after it.
 * This should throw IllegalStateException.
 */
@Command("invalid-greedy-with-optional")
public class InvalidGreedyWithOptional {

    @Execute
    public void execute(TestSource source, 
                       @Greedy String message, 
                       @Optional String suffix) {
        source.reply("Message: " + message + ", Suffix: " + suffix);
    }
}

