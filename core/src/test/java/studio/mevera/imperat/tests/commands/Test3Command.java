package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.command.AttachmentMode;
import studio.mevera.imperat.tests.TestSource;

@Command("test3")
public class Test3Command {

    @Execute
    public void def(TestSource source, @Named("input") @Default("hello") String input) {
        source.reply("input=" + input);
    }

    @SubCommand(value = "sub", attachment = AttachmentMode.EMPTY)
    public void subDefaultExecution(TestSource source) {
        source.reply("subcommand - default execution !");
    }

    @SubCommand(value = "sub", attachment = AttachmentMode.EMPTY)
    public void subMainExecution(TestSource source, @Named("sub-input") String subInput) {
        source.reply("sub command input= " + subInput);
    }
    /*
    @SubCommand(value = "sub", attachment = AttachmentMode.EMPTY)
    public static class Sub {
        @Usage
        public void def(TestSource source) {
            source.reply("sub command - default execution");
        }
        @Usage
        public void sub(TestSource source, @Named("sub-input") String subInput) {
            source.reply("sub command input= " + subInput);
        }
    }*/
}