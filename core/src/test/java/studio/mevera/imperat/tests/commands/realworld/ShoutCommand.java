package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Switch;
import studio.mevera.imperat.tests.TestSource;

/**
 * /shout <message...> [-loud/-l] [-bold/-b]
 * <p>
 * A command where switches are declared AFTER the greedy string parameter.
 * Used to test that the greedy argument stops consuming when it encounters
 * switch tokens in the raw input, regardless of declaration order.
 */
@RootCommand("shout")
public class ShoutCommand {

    @Execute
    public void shout(
            TestSource source,
            @Named("message") @Greedy String message,
            @Switch({"loud", "l"}) boolean loud,
            @Switch({"bold", "b"}) boolean bold
    ) {
        source.reply("message=" + message);
        source.reply("loud=" + loud);
        source.reply("bold=" + bold);
    }
}

