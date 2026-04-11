package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("aliascmd")
public class SubcommandAliasSuggestionCommand {

    @SubCommand({"manage", "m", "mgr"})
    @Execute
    public void manage(TestCommandSource source) {
    }

    @SubCommand({"profile", "p", "prof"})
    @Execute
    public void profile(TestCommandSource source) {
    }
}
