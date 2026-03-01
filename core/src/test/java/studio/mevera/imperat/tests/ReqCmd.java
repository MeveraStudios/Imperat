package studio.mevera.imperat.tests;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;

@RootCommand("req")
public class ReqCmd {

    @Execute
    public void exec(TestSource source, String a, String b, String c, String d) {
        source.reply("ReqCmd executed with a=" + a + ", b=" + b + ", c=" + c + ", d=" + d);
    }

}
