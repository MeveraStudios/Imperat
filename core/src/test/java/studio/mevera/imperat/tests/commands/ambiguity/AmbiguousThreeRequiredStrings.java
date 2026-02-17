package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Three execute methods with conflicting first parameter (all required String).
 * At the first argument position, the framework cannot distinguish between
 * "first", "second", and "third" since all are required strings at the same level.
 */
@Command("ambiguous-three-required-strings")
public class AmbiguousThreeRequiredStrings {

    @Execute
    public void withFirst(TestSource source, String first) {
        source.reply("First: " + first);
    }

    @Execute
    public void withSecond(TestSource source, String second) {
        source.reply("Second: " + second);
    }

    @Execute
    public void withThird(TestSource source, String third) {
        source.reply("Third: " + third);
    }
}

