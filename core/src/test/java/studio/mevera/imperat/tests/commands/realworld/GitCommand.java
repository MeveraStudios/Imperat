package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Flag;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@Command("git")
public class GitCommand {

    @Execute
    public void def(TestSource source) {
        source.reply("default usage");
    }

    @SubCommand("commit")
    public void commit(TestSource source, @Flag({"message", "m"}) String msg) {

        // /git commit -m <message>
        System.out.println("Commiting with msg: " + msg);

    }

}