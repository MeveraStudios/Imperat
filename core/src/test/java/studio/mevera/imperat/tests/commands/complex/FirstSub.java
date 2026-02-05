package studio.mevera.imperat.tests.commands.complex;

import studio.mevera.imperat.annotations.ExternalSubCommand;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.SubCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.tests.TestSource;

@SubCommand("first")
@ExternalSubCommand(SecondSub.class)
public final class FirstSub {

    @Execute
    public void defaultUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2) {
        source.reply("Default execution of first sub-command");
    }

    @Execute
    public void cmdUsage(TestSource source, @Named("otherText") String otherText, @Named("otherText2") String otherText2,
            @Named("arg1") @Suggest({"x", "y", "z", "sexy"}) String arg1) {
        source.reply("Executing usage in first's main usage, otherText=" + otherText + ", arg1= " + arg1);
    }

}