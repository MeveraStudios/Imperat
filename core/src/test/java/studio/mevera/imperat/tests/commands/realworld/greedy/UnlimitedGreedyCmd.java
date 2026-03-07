package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/** /unlimited <text> — greedy with no limit, consumes everything */
@RootCommand("unlimited")
public class UnlimitedGreedyCmd {

    @Execute
    public void exec(TestCommandSource source,
            @Named("text") @Greedy String text) {
        source.reply("text=" + text);
    }
}

