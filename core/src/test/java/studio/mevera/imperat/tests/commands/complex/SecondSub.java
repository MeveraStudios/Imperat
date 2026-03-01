package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@SubCommand(value = "second", attachTo = "<arg1>")
public class SecondSub {

    @Execute
    public void defaultUsage(TestSource source,
            @InheritedArg @Named("otherText") String otherText,
            @InheritedArg @Named("otherText2") String otherText2,
            @InheritedArg @Named("arg1") String arg1
    ) {
        source.reply("Default execution of second sub-command");
    }

    @Execute
    public void cmdUsage(TestSource source,
            @InheritedArg @Named("otherText") String otherText,
            @InheritedArg @Named("otherText2") String otherText2,
            @InheritedArg @Named("arg1") String arg1,
            @Named("arg2") String arg2) {
        source.reply("Executing usage in first's main usage," +
                             " otherText=" + otherText + ", arg1= " + arg1 + ", arg2= " + arg2);
    }

}