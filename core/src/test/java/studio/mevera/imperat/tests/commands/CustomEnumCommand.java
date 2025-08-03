package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.Command;
import studio.mevera.imperat.annotations.Named;
import studio.mevera.imperat.annotations.Usage;
import studio.mevera.imperat.tests.TestSource;
import studio.mevera.imperat.tests.parameters.CustomEnum;

@Command("customenum")
public class CustomEnumCommand {

    @Usage
    public void exec(TestSource source, @Named("enumHere") CustomEnum customEnum) {
        source.reply("Custom enum input: " + customEnum.name());
    }

}