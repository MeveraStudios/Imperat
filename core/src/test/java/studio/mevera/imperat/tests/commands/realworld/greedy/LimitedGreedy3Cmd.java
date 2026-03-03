package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/** /limited3 <phrase> — greedy with limit=3, captures up to 3 tokens */
@RootCommand("limited3")
public class LimitedGreedy3Cmd {

    @Execute
    public void exec(TestSource source,
            @Named("phrase") @Greedy(limit = 3) String phrase) {
        source.reply("phrase=" + phrase);
    }
}

