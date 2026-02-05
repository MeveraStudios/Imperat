package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@Command("mv")
public class MultipleVariantsCmd {

    @Execute
    public void v1(TestSource source, String str1) {
        source.reply("Executed variant one with str1=" + str1);
    }

    @Execute
    public void v2(TestSource source, int i1) {
        source.reply("Executed variant two with int1=" + i1);
    }

}
