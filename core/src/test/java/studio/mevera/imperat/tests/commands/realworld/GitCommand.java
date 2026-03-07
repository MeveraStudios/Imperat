package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Flag;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("git")
public class GitCommand {

    @Execute
    public void def(TestCommandSource source) {
    }

    @SubCommand("commit")
    public void commit(TestCommandSource source, @Flag({"message", "m"}) String msg) {
        // /git commit -m <message>
        System.out.println("Commiting with msg: " + msg);
    }

}