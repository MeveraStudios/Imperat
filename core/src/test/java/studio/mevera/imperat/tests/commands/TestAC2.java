package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.*;
import studio.mevera.imperat.tests.TestSource;

@Command("testac2")
public class TestAC2 {

    @Usage
    public void onUsage(
            TestSource source,
            @Suggest("any_text") @Named("text") String text,
            @Default("1") @Suggest({"2", "5", "10"}) @Named("count") Integer count,
            @Default("2") @Suggest({"3.1", "6.2", "9.5"}) @Named("extra") Double extra
    ) {
        // /testac <text> [count] [extra]
    }
}