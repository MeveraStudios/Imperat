package studio.mevera.imperat.tests.commands;

import org.jetbrains.annotations.NotNull;
import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("opt")
public class OptionalArgCommand {

    @Usage
    public void mainUsage(TestSource source, @Named("a") String arg1, @Named("b") @Default("hello-world") @NotNull String b) {
        source.reply("A=" + arg1 + ", B= " + b);
    }

}