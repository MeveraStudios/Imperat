package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Optional;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("foa")
public final class FirstOptionalArgumentCmd {

    @Usage
    public void def(TestSource source, @Named("num") @Optional @Default("1") Integer num) {
        source.reply("Num=" + num);
    }

    @SubCommand("sub")
    public static class MySub {


        @Usage
        public void defaultUsage(TestSource source, @Named("num") Integer num) {
            source.reply("Default execution of sub-command, inherited num='" + num + "'");
        }

        @Usage
        public void mainUsage(TestSource source, @Named("num") Integer num, @Named("num2") Integer num2) {
            source.reply("Main execution of sub-command, inherited num='" + num + "', num2='" + num2 + "'");
        }

    }


}