package studio.mevera.imperat.tests.commands;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("opt")
public class OptionalArgCommand {

    @Execute
    public void mainUsage(TestSource source, @Named("a") String arg1, @Named("b") @Default("hello-world") @NotNull String b) {
        source.reply("A=" + arg1 + ", B= " + b);
    }

}