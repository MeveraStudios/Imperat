package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

@Command({"message"})
public class MessageCmd {

    @Usage
    public void exec(@NotNull TestSource sender,
                     @Named("target") @NotNull String target,
                     @Named("message") @Suggest({"this is a long greedy", "some sentence", "idk"}) @Greedy String message) {
        sender.reply("sending to '" + target +
            "' the message '" + message + "'");
    }

}