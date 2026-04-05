package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.greedy_type_example.Message;

@RootCommand({"broadcastmessage", "bm"})
public class BroadcastMessage {

    @Execute
    public void exec(TestCommandSource src, Message msg) {
        src.reply("Msg: '" + msg.getMessage() + "'");
    }


}
