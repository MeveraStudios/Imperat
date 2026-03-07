package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * VALID: Multiple execute methods with optional parameters of different types is NOT ambiguous.
 * The framework can distinguish between String, Integer, Double, and Boolean types.
 */
@RootCommand("valid-multiple-optional-different-types")
public class ValidMultipleOptionalDifferentTypes {

    @Execute
    public void withString(TestCommandSource source, @Optional String name) {
        source.reply("Name: " + name);
    }

    @Execute
    public void withInteger(TestCommandSource source, @Optional Integer age) {
        source.reply("Age: " + age);
    }

    @Execute
    public void withDouble(TestCommandSource source, @Optional Double balance) {
        source.reply("Balance: " + balance);
    }

    @Execute
    public void withBoolean(TestCommandSource source, @Optional Boolean active) {
        source.reply("Active: " + active);
    }
}

