package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * VALID: Two execute methods with different types at the first parameter is NOT ambiguous.
 * The framework can distinguish between String and int types.
 */
@RootCommand("valid-different-types")
public class ValidDifferentTypes {

    @Execute
    public void withString(TestCommandSource source, String name) {
        source.reply("Name: " + name);
    }

    @Execute
    public void withInt(TestCommandSource source, int age) {
        source.reply("Age: " + age);
    }
}

