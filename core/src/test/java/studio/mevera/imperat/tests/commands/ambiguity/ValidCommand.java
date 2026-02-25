package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * A valid command with no ambiguity issues.
 * Different types for required parameters.
 */
@RootCommand("validcmd")
public class ValidCommand {

    @Execute
    public void execute(TestSource source) {
        source.reply("Valid command executed");
    }

    @Execute
    public void withArgs(TestSource source, String name, int age) {
        source.reply("Name: " + name + ", Age: " + age);
    }
}

