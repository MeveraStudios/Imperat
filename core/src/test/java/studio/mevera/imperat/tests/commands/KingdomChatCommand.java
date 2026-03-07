package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Greedy;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("kingdomchat")
public class KingdomChatCommand {

    @Execute
    public void def(TestCommandSource source) {
        source.reply("This is the default usage of the kingdomchat command.");
    }

    @Execute
    public void mainUsage(TestCommandSource source, @Named("message") @Greedy String message) {
        source.reply("Your message: " + message);
    }
}