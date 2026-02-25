package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("test3")
public class Test3Command {

    @Execute
    public void def(TestSource source, @Named("input") @Default("hello") String input) {
        source.reply("input=" + input);
    }

    @SubCommand(value = "sub")
    public void subDefaultExecution(TestSource source) {
        source.reply("subcommand - default execution !");
    }

    @SubCommand(value = "sub")
    public void subMainExecution(TestSource source, @Named("sub-input") String subInput) {
        source.reply("sub command input= " + subInput);
    }

}