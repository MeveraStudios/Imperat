package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("mv")
public class MultipleVariantsCmd {

    @Usage
    public void v1(TestSource source, String str1) {
        source.reply("Executed variant one with str1=" + str1);
    }

    @Usage
    public void v2(TestSource source, int i1) {
        source.reply("Executed variant two with int1=" + i1);
    }

}
