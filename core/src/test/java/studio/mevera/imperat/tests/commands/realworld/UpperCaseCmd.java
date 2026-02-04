package studio.mevera.imperat.tests.commands.realworld;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("upper_case")
public class UpperCaseCmd {

    @Usage
    public void t(TestSource source) {
        source.reply("WORKED !");
    }

}
