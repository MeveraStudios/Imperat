package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.Named;
import studio.mevera.imperat.annotations.types.RootCommand;
import studio.mevera.imperat.tests.TestCommandSource;
import studio.mevera.imperat.tests.parameters.CustomEnum;

@RootCommand("customenum")
public class CustomEnumCommand {

    @Execute
    public void exec(TestCommandSource source, @Named("enumHere") CustomEnum customEnum) {
        source.reply("Custom enum input: " + customEnum.name());
    }

}