package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Two execute methods with conflicting first parameter (both required int).
 * At the first argument position, the framework cannot distinguish between
 * "count" and "limit" since both are required integers at the same level.
 */
@RootCommand("ambiguous-same-type-int")
public class AmbiguousSameTypeInt {

    @Execute
    public void withCount(TestSource source, int count) {
        source.reply("Count: " + count);
    }

    @Execute
    public void withLimit(TestSource source, int limit) {
        source.reply("Limit: " + limit);
    }
}

