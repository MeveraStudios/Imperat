package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Four execute methods with conflicting first parameter (all optional Boolean).
 * At the first argument position, the framework cannot distinguish between
 * "flag1", "flag2", "flag3", and "flag4" since all are optional booleans at the same level.
 */
@RootCommand("ambiguous-four-booleans")
public class AmbiguousFourOptionalBooleans {

    @Execute
    public void withFlag1(TestSource source, @Optional Boolean flag1) {
        source.reply("Flag1: " + flag1);
    }

    @Execute
    public void withFlag2(TestSource source, @Optional Boolean flag2) {
        source.reply("Flag2: " + flag2);
    }

    @Execute
    public void withFlag3(TestSource source, @Optional Boolean flag3) {
        source.reply("Flag3: " + flag3);
    }

    @Execute
    public void withFlag4(TestSource source, @Optional Boolean flag4) {
        source.reply("Flag4: " + flag4);
    }
}

