package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Greedy;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@Command("kingdomchat")
public class KingdomChatCommand {

    @Execute
    public void def(TestSource source) {
        source.reply("This is the default usage of the kingdomchat command.");
    }

    @Execute
    public void mainUsage(TestSource source, @Named("message") @Greedy String message) {
        source.reply("Your message: " + message);
    }
}