package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.RootCommand;
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

