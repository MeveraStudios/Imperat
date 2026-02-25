package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.tests.TestSource;

@RootCommand({"message"})
public class MessageCmd {

    @Execute
    public void exec(@NotNull TestSource sender,
            @Named("target") @NotNull String target,
            @Named("message") @Suggest({"this is a long greedy", "some sentence", "idk"}) @Greedy String message) {
        sender.reply("sending to '" + target +
                             "' the message '" + message + "'");
    }

}