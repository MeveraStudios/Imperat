package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestCommandSource;

@MyCustomAnnotation(name = "testreplacer")
public class TestCustomAnnotationCmd {

    @Execute
    public void def(TestCommandSource source) {
        source.reply("DEF");
    }

    @SubCommand("teto")
    interface Teto {

    }
}