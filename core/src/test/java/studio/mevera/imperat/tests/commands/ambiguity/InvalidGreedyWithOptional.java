package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * INVALID: Greedy parameter with another parameter after it.
 * This should throw IllegalStateException.
 */
@RootCommand("invalid-greedy-with-optional")
public class InvalidGreedyWithOptional {

    @Execute
    public void execute(TestSource source, 
                       @Greedy String message, 
                       @Optional String suffix) {
        source.reply("Message: " + message + ", Suffix: " + suffix);
    }
}

