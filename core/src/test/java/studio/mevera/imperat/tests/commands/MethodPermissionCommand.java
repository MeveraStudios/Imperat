package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Permission;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@RootCommand("methodperm")
public class MethodPermissionCommand {

    @Execute
    @Permission("methodperm.use")
    public void execute(TestCommandSource source) {
        source.reply("methodperm");
    }
}
