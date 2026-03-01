package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * VALID: Two execute methods with mixed nature (required vs optional) is NOT ambiguous
 * because they have different nature, even with same type.
 * The framework can distinguish based on whether the argument is provided or not.
 */
@RootCommand("valid-mixed-nature")
public class ValidMixedNature {

    @Execute
    public void withRequired(TestSource source, String required) {
        source.reply("Required: " + required);
    }

    @Execute
    public void withOptional(TestSource source, @Optional String optional) {
        source.reply("Optional: " + optional);
    }
}

