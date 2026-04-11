package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.Suggest;
import studio.mevera.imperat.tests.TestCommandSource;

public final class SuggestionFallbackCommands {

    @RootCommand("fallbackcmd")
    public void fallback(
            TestCommandSource source,
            @studio.mevera.imperat.annotations.types.SuggestionProvider(EmptyTestSuggestionProvider.class) String value
    ) {
    }

    @RootCommand("primarycmd")
    public void primary(
            TestCommandSource source,
            @Suggest({"primary-one", "primary-two"}) String value
    ) {
    }
}
