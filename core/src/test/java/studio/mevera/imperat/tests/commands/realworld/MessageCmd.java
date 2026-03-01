package studio.mevera.imperat.tests.commands.realworld;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Suggest;
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