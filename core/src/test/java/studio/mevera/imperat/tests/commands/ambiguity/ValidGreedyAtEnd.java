package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * VALID: Greedy parameter at the end is valid.
 */
@RootCommand("valid-greedy-last")
public class ValidGreedyAtEnd {

    @Execute
    public void execute(TestSource source, String name, @Greedy String message) {
        source.reply("Name: " + name + ", Message: " + message);
    }
}

