package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Default;
import studio.mevera.imperat.annotations.Execute;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.RootCommand;
import studio.mevera.imperat.annotations.Suggest;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("testac")
public class TestAC {

    @Execute
    public void onUsage(
            TestSource source,
            @Suggest("any_text") @Named("text") String text,
            @Default("1") @Suggest({"2", "5", "10"}) @Named("count") Integer count
    ) {
        // /testac <text> [count]
    }
}