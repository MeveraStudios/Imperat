package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

/** /limited1 <word> — greedy with limit=1, captures exactly one token */
@RootCommand("limited1")
public class LimitedGreedy1Cmd {

    @Execute
    public void exec(TestCommandSource source,
            @Named("word") @Greedy(limit = 1) String word) {
        source.reply("word=" + word);
    }
}

