package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Permission;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("testperm")
@Permission("testperm.use")
public class TestPerm {
    
    @Usage
    public void def(TestSource source) {
        source.reply("DEFAULT-EXE");
    }
    
    @Usage
    public void mainUsage(TestSource source, String a, String b, @Default("1") Integer c) {
        source.reply("a=" + a +", b=" + b + ", c=" + c);
    }
    
}
