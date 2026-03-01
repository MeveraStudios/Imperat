package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.Optional;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("foa")
public final class FirstOptionalArgumentCmd {

    @Execute
    public void def(TestSource source, @Named("num") @Optional @Default("1") Integer num) {
        source.reply("Num=" + num);
    }

    @SubCommand(value = "sub", attachTo = "[num]")
    public static class MySub {


        @Execute
        public void defaultUsage(TestSource source, @InheritedArg @Named("num") Integer num) {
            source.reply("Default execution of sub-command, inherited num='" + num + "'");
        }

        @Execute
        public void mainUsage(TestSource source, @InheritedArg @Named("num") Integer num, @Named("num2") Integer num2) {
            source.reply("Main execution of sub-command, inherited num='" + num + "', num2='" + num2 + "'");
        }

    }


}