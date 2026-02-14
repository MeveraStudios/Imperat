package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Three execute methods with conflicting first parameter (all optional String).
 * At the first argument position, the framework cannot distinguish between
 * "first", "second", and "third" since all are optional strings at the same level.
 */
@Command("ambiguous-three-optional")
public class AmbiguousThreeOptional {

    @Execute
    public void withFirst(TestSource source, @Optional String first) {
        source.reply("First: " + first);
    }

    @Execute
    public void withSecond(TestSource source, @Optional String second) {
        source.reply("Second: " + second);
    }

    @Execute
    public void withThird(TestSource source, @Optional String third) {
        source.reply("Third: " + third);
    }
}

