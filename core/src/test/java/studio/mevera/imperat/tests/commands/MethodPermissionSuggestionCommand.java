package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("permcomplete")
public class MethodPermissionSuggestionCommand {

    @Execute
    @SubCommand("open")
    public void open(TestCommandSource source) {
        source.reply("open");
    }

    @Execute
    @SubCommand("restricted")
    @Permission("permcomplete.restricted")
    public void restricted(TestCommandSource source) {
        source.reply("restricted");
    }
}
