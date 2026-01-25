package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;

@Command("testac")
public class TestAC {

    @Usage
    public void onUsage(
            TestSource source,
            @Suggest("any_text") @Named("text") String text,
            @Default("1") @Suggest({"2", "5", "10"}) @Named("count") Integer count
    ) {
        // /testac <text> [count]
    }
}