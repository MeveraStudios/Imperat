package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * AMBIGUOUS: Three execute methods with conflicting first parameter (all required String).
 * At the first argument position, the framework cannot distinguish between
 * "first", "second", and "third" since all are required strings at the same level.
 */
@RootCommand("ambiguous-three-required-strings")
public class AmbiguousThreeRequiredStrings {

    @Execute
    public void withFirst(TestCommandSource source, String first) {
        source.reply("First: " + first);
    }

    @Execute
    public void withSecond(TestCommandSource source, String second) {
        source.reply("Second: " + second);
    }

    @Execute
    public void withThird(TestCommandSource source, String third) {
        source.reply("Third: " + third);
    }
}

