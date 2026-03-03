package studio.mevera.imperat.tests.commands.realworld.greedy;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

/**
 * /broadcast <message...(limit=3)> <repeat:int>
 * message is greedy(limit=3), repeat is an int — the greedy should yield
 * a token to repeat when it matches the int type.
 */
@RootCommand("broadcast")
public class LimitedGreedyWithTypedTrailingArgCmd {

    @Execute
    public void exec(TestSource source,
            @Named("message") @Greedy(limit = 3) String message,
            @Named("repeat") int repeat) {
        source.reply("message=" + message + " repeat=" + repeat);
    }
}

