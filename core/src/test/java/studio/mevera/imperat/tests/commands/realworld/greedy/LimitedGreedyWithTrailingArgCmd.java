package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/**
 * /mixed <target> <prefix> <suffix>
 * prefix is greedy(limit=2), suffix is a normal required arg after it.
 * Tests that a limited greedy doesn't swallow the trailing required arg.
 */
@RootCommand("mixed")
public class LimitedGreedyWithTrailingArgCmd {

    @Execute
    public void exec(TestCommandSource source,
            @Named("target") String target,
            @Named("prefix") @Greedy(limit = 2) String prefix,
            @Named("suffix") String suffix) {
        source.reply("target=" + target + " prefix=" + prefix + " suffix=" + suffix);
    }
}

