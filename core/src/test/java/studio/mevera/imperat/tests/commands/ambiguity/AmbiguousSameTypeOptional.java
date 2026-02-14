package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Two execute methods with conflicting first parameter (both optional String).
 * At the first argument position, the framework cannot distinguish between
 * "param1" and "param2" since both are optional strings at the same level.
 */
@Command("ambiguous-same-type-optional")
public class AmbiguousSameTypeOptional {

    @Execute
    public void withParam1(TestSource source, @Optional String param1) {
        source.reply("Param1: " + param1);
    }

    @Execute
    public void withParam2(TestSource source, @Optional String param2) {
        source.reply("Param2: " + param2);
    }
}

