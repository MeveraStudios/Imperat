package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@SubCommand("second")
public class SecondSub {

    @Usage
    public void defaultUsage(TestSource source,
                             @Named("otherText") String otherText,
                             @Named("otherText2") String otherText2,
                             @Named("arg1") String arg1
    ) {
        source.reply("Default execution of second sub-command");
    }

    @Usage
    public void cmdUsage(TestSource source,
                         @Named("otherText") String otherText,
                         @Named("otherText2") String otherText2,
                         @Named("arg1") String arg1,
                         @Named("arg1") String arg2) {
        source.reply("Executing usage in first's main usage," +
            " otherText=" + otherText + ", arg1= " + arg1 + ", arg2= " + arg2);
    }

}