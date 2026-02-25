package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * AMBIGUOUS: Two execute methods with conflicting first parameter (both required String).
 * At the first argument position, the framework cannot distinguish between
 * "name" and "title" since both are required strings at the same level.
 */
@RootCommand("ambiguous-same-type-required")
public class AmbiguousSameTypeRequired {

    @Execute
    public void withName(TestSource source, String name) {
        source.reply("Name: " + name);
    }

    @Execute
    public void withTitle(TestSource source, String title) {
        source.reply("Title: " + title);
    }
}

