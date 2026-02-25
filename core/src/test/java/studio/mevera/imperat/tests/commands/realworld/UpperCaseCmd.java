package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("upper_case")
public class UpperCaseCmd {

    @Execute
    public void t(TestSource source) {
        source.reply("WORKED !");
    }

}
