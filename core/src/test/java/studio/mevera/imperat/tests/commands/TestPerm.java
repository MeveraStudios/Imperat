package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@Command("testperm")
@Permission("testperm.use")
public class TestPerm {

    @Execute
    public void def(TestSource source) {
        source.reply("DEFAULT-EXE");
    }

    @Execute
    public void mainUsage(TestSource source, @Permission("testperm.a.use") String a, String b, @Default("1") Integer c) {
        source.reply("a=" + a + ", b=" + b + ", c=" + c);
    }

}
