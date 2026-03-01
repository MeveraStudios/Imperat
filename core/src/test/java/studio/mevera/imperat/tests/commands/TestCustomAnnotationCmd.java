package studio.mevera.imperat.tests.commands;

import studio.mevera.imperat.annotations.types.Execute;
import studio.mevera.imperat.annotations.types.SubCommand;
import studio.mevera.imperat.tests.TestSource;

@MyCustomAnnotation(name = "testreplacer")
public class TestCustomAnnotationCmd {

    @Execute
    public void def(TestSource source) {
        source.reply("DEF");
    }

    @SubCommand("teto")
    interface Teto {

    }
}