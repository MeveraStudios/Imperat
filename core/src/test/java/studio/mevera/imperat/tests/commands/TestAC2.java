package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Default;
import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestSource;

@RootCommand("testac2")
public class TestAC2 {

    @Execute
    public void onUsage(
            TestSource source,
            @Suggest("any_text") @Named("text") String text,
            @Default("1") @Suggest({"2", "5", "10"}) @Named("count") Integer count,
            @Default("2") @Suggest({"3.1", "6.2", "9.5"}) @Named("extra") Double extra
    ) {
        // /testac <text> [count] [extra]
    }
}