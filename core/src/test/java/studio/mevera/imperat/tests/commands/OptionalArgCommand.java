package studio.mevera.imperat.tests.commands;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("opt")
public class OptionalArgCommand {

    @Execute
    public void mainUsage(TestSource source, @Named("a") String arg1, @Named("b") @Default("hello-world") @NotNull String b) {
        source.reply("A=" + arg1 + ", B= " + b);
    }

}