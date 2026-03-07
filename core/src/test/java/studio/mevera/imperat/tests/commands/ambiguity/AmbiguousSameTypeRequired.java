package studio.mevera.imperat.tests.commands.ambiguity;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * AMBIGUOUS: Two execute methods with conflicting first parameter (both required String).
 * At the first argument position, the framework cannot distinguish between
 * "name" and "title" since both are required strings at the same level.
 */
@RootCommand("ambiguous-same-type-required")
public class AmbiguousSameTypeRequired {

    @Execute
    public void withName(TestCommandSource source, String name) {
        source.reply("Name: " + name);
    }

    @Execute
    public void withTitle(TestCommandSource source, String title) {
        source.reply("Title: " + title);
    }
}

