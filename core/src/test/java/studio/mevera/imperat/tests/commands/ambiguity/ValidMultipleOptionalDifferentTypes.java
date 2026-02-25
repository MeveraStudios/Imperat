package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * VALID: Multiple execute methods with optional parameters of different types is NOT ambiguous.
 * The framework can distinguish between String, Integer, Double, and Boolean types.
 */
@RootCommand("valid-multiple-optional-different-types")
public class ValidMultipleOptionalDifferentTypes {

    @Execute
    public void withString(TestSource source, @Optional String name) {
        source.reply("Name: " + name);
    }

    @Execute
    public void withInteger(TestSource source, @Optional Integer age) {
        source.reply("Age: " + age);
    }

    @Execute
    public void withDouble(TestSource source, @Optional Double balance) {
        source.reply("Balance: " + balance);
    }

    @Execute
    public void withBoolean(TestSource source, @Optional Boolean active) {
        source.reply("Active: " + active);
    }
}

