package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.ExternalSubCommand;
import studio.mevera.imperat.annotations.types.InheritedArg;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestSource;

@SubCommand(value = "first", attachTo = "<otherText2>")
@ExternalSubCommand({SecondSub.class})
public final class FirstSub {

    @Execute
    public void defaultUsage(TestSource source, @InheritedArg @Named("otherText") String otherText,
            @InheritedArg @Named("otherText2") String otherText2) {
        source.reply("Default execution of first sub-command");
    }

    @Execute
    public void cmdUsage(TestSource source, @InheritedArg @Named("otherText") String otherText, @InheritedArg @Named("otherText2") String otherText2,
            @Named("arg1") @Suggest({"x", "y", "z", "sexy"}) String arg1) {
        source.reply("Executing usage in first's main usage, otherText=" + otherText + ", arg1= " + arg1);
    }

}